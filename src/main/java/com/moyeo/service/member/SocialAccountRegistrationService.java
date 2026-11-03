package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import com.moyeo.domain.member.SocialAccount;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.member.KakaoUnlinkTaskRepository;
import com.moyeo.repository.member.AppleRevokeTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SocialAccountRegistrationService {

    private final UserRepository userRepository;
    private final KakaoUnlinkTaskRepository kakaoUnlinkTaskRepository;
    private final AppleRevokeTaskRepository appleRevokeTaskRepository;
    private final SocialAccountRepository socialAccountRepository;

    public SocialAccountRegistrationService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            KakaoUnlinkTaskRepository kakaoUnlinkTaskRepository,
            AppleRevokeTaskRepository appleRevokeTaskRepository
    ) {
        this.userRepository = userRepository;
        this.kakaoUnlinkTaskRepository = kakaoUnlinkTaskRepository;
        this.appleRevokeTaskRepository = appleRevokeTaskRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuthenticatedMember register(AuthProvider provider, String providerUserId) {
        return register(provider, providerUserId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuthenticatedMember register(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext
    ) {
        return register(provider, providerUserId, providerRefreshTokenCiphertext, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuthenticatedMember register(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient
    ) {
        return register(provider, providerUserId, providerRefreshTokenCiphertext, appleRefreshTokenClient, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuthenticatedMember registerKakao(String providerUserId, Runnable verifyNewRegistration) {
        return register(AuthProvider.KAKAO, providerUserId, null, null, verifyNewRegistration);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuthenticatedMember registerApple(String providerUserId, String ciphertext,
            AppleRefreshTokenClient client, Runnable verifyNewRegistration) {
        return register(AuthProvider.APPLE, providerUserId, ciphertext, client, verifyNewRegistration);
    }

    private AuthenticatedMember register(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient,
            Runnable verifyNewRegistration
    ) {
        validateProviderToken(provider, providerRefreshTokenCiphertext);
        User user = userRepository.save(User.pendingOnboarding());
        SocialAccount socialAccount = new SocialAccount(
                user,
                provider,
                providerUserId,
                null,
                providerRefreshTokenCiphertext,
                appleRefreshTokenClient
        );
        socialAccountRepository.saveAndFlush(socialAccount);
        if ((provider == AuthProvider.KAKAO
                && kakaoUnlinkTaskRepository.findByProviderUserIdForUpdate(providerUserId).isPresent())
                || (provider == AuthProvider.APPLE
                && appleRevokeTaskRepository.findByProviderUserIdForUpdate(providerUserId).isPresent())) {
            // Locking/current read covers withdrawal committing while registration was starting.
            throw new MoyeoException(AuthenticationErrorCode.SOCIAL_DISCONNECTION_PENDING);
        }
        // Cleanup may have finished between the initial provider lookup and this transaction.
        // Recheck the provider token after the task gate and before committing a new membership.
        if (verifyNewRegistration != null) verifyNewRegistration.run();
        return AuthenticatedMember.from(user, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuthenticatedMember> findRegistered(AuthProvider provider, String providerUserId) {
        return findRegistered(provider, providerUserId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuthenticatedMember> findRegistered(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext
    ) {
        return findRegistered(provider, providerUserId, providerRefreshTokenCiphertext, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuthenticatedMember> findRegistered(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient
    ) {
        if ((provider == AuthProvider.KAKAO
                && kakaoUnlinkTaskRepository.findByProviderUserIdForUpdate(providerUserId).isPresent())
                || (provider == AuthProvider.APPLE
                && appleRevokeTaskRepository.findByProviderUserIdForUpdate(providerUserId).isPresent())) {
            throw new MoyeoException(AuthenticationErrorCode.SOCIAL_DISCONNECTION_PENDING);
        }
        validateProviderToken(provider, providerRefreshTokenCiphertext);
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(socialAccount -> {
                    User user = socialAccount.getUser();
                    if (user.getDeletedAt() != null) {
                        throw new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED);
                    }
                    if (providerRefreshTokenCiphertext != null) {
                        if (appleRefreshTokenClient == null) {
                            socialAccount.updateProviderRefreshTokenCiphertext(providerRefreshTokenCiphertext);
                        } else {
                            socialAccount.updateAppleRefreshToken(providerRefreshTokenCiphertext, appleRefreshTokenClient);
                        }
                    }
                    return AuthenticatedMember.from(user, false);
                });
    }
    private void validateProviderToken(AuthProvider provider, String ciphertext) {
        if (provider == AuthProvider.APPLE && (ciphertext == null || ciphertext.isBlank())) {
            throw new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE);
        }
    }

}
