package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.member.KakaoUnlinkTaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberAuthService {

    private final UserRepository userRepository;
    private final KakaoUnlinkTaskRepository kakaoUnlinkTaskRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialAccountRegistrationService socialAccountRegistrationService;

    public MemberAuthService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            SocialAccountRegistrationService socialAccountRegistrationService,
            KakaoUnlinkTaskRepository kakaoUnlinkTaskRepository
    ) {
        this.userRepository = userRepository;
        this.kakaoUnlinkTaskRepository = kakaoUnlinkTaskRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.socialAccountRegistrationService = socialAccountRegistrationService;
    }

    public AuthenticatedMember findAuthenticatedMember(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        return AuthenticatedMember.from(user, false);
    }

    @Transactional
    public AuthenticatedMember loginSocial(
            AuthProvider provider,
            String providerUserId
    ) {
        return loginSocial(provider, providerUserId, null);
    }

    @Transactional
    public AuthenticatedMember loginSocial(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext
    ) {
        return loginSocial(provider, providerUserId, providerRefreshTokenCiphertext, null);
    }

    @Transactional
    public AuthenticatedMember loginSocial(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient
    ) {
        return loginSocial(provider, providerUserId, providerRefreshTokenCiphertext, appleRefreshTokenClient, null);
    }

    @Transactional
    public AuthenticatedMember loginKakao(String providerUserId, Runnable verifyNewRegistration) {
        return loginSocial(AuthProvider.KAKAO, providerUserId, null, null, verifyNewRegistration);
    }

    private AuthenticatedMember loginSocial(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient,
            Runnable verifyNewRegistration
    ) {
        if (provider == AuthProvider.KAKAO && kakaoUnlinkTaskRepository.existsByProviderUserId(providerUserId)) {
            throw new MoyeoException(AuthenticationErrorCode.SOCIAL_DISCONNECTION_PENDING);
        }
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(socialAccount -> {
                    if (provider == AuthProvider.KAKAO) {
                        // Serialize login with withdrawal before issuing a new service session.
                        userRepository.findActiveByIdForUpdate(socialAccount.getUser().getId())
                                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED));
                    }
                    if (providerRefreshTokenCiphertext != null) {
                        if (appleRefreshTokenClient == null) {
                            socialAccount.updateProviderRefreshTokenCiphertext(providerRefreshTokenCiphertext);
                        } else {
                            socialAccount.updateAppleRefreshToken(providerRefreshTokenCiphertext, appleRefreshTokenClient);
                        }
                    }
                    return authenticatedLoginMember(socialAccount.getUser());
                })
                .orElseGet(() -> registerOrRecoverConcurrentLogin(
                        provider,
                        providerUserId,
                        providerRefreshTokenCiphertext,
                        appleRefreshTokenClient,
                        verifyNewRegistration
                ));
    }

    private AuthenticatedMember authenticatedLoginMember(User user) {
        if (user.getDeletedAt() != null) {
            throw new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return AuthenticatedMember.from(user, false);
    }

    private AuthenticatedMember registerOrRecoverConcurrentLogin(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient,
            Runnable verifyNewRegistration
    ) {
        try {
            if (provider == AuthProvider.KAKAO && verifyNewRegistration != null) {
                return socialAccountRegistrationService.registerKakao(providerUserId, verifyNewRegistration);
            }
            return socialAccountRegistrationService.register(
                    provider,
                    providerUserId,
                    providerRefreshTokenCiphertext,
                    appleRefreshTokenClient
            );
        } catch (DataIntegrityViolationException exception) {
            return socialAccountRegistrationService.findRegistered(
                            provider,
                            providerUserId,
                            providerRefreshTokenCiphertext,
                            appleRefreshTokenClient
                    )
                    .orElseThrow(() -> exception);
        }
    }
}
