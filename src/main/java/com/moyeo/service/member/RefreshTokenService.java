package com.moyeo.service.member;

import com.moyeo.domain.member.RefreshToken;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.global.security.RefreshTokenProperties;
import com.moyeo.repository.member.RefreshTokenRepository;
import com.moyeo.repository.member.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenProperties properties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, RefreshTokenProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional
    public SessionToken issue(Long userId) {
        return createAndSave(userRepository.getReferenceById(userId));
    }

    @Transactional
    public RefreshSession refresh(String presentedToken) {
        String tokenHash = hash(presentedToken);
        RefreshToken candidate = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::authenticationRequired);
        User user = userRepository.findActiveByIdForUpdate(candidate.getUserId())
                .orElseThrow(this::authenticationRequired);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::authenticationRequired);
        if (refreshToken.isExpiredAt(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw authenticationRequired();
        }
        String replacementToken = createToken();
        refreshToken.rotate(
                hash(replacementToken),
                LocalDateTime.now().plusSeconds(properties.refreshTokenValiditySeconds())
        );
        return new RefreshSession(AuthenticatedMember.from(user, false), replacementToken, refreshToken.getSessionId());
    }

    @Transactional
    public void logout(String presentedToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashForUpdate(hash(presentedToken))
                .orElseThrow(this::authenticationRequired);
        if (refreshToken.isExpiredAt(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw authenticationRequired();
        }
        refreshTokenRepository.delete(refreshToken);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        refreshTokenRepository.deleteAllByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public boolean isActiveSession(Long userId, String sessionId) {
        return refreshTokenRepository.existsByUser_IdAndSessionIdAndExpiresAtAfter(userId, sessionId, LocalDateTime.now());
    }

    private SessionToken createAndSave(User user) {
        return createAndSave(user, UUID.randomUUID().toString());
    }

    private SessionToken createAndSave(User user, String sessionId) {
        String token = createToken();
        refreshTokenRepository.save(new RefreshToken(
                user,
                hash(token),
                sessionId,
                LocalDateTime.now().plusSeconds(properties.refreshTokenValiditySeconds())
        ));
        return new SessionToken(token, sessionId);
    }

    private String createToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash refresh token", exception);
        }
    }

    private MoyeoException authenticationRequired() {
        return new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED);
    }

    public record SessionToken(String refreshToken, String sessionId) {
    }

    public record RefreshSession(AuthenticatedMember member, String refreshToken, String sessionId) {
    }
}
