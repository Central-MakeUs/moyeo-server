package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberAuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialAccountRegistrationService socialAccountRegistrationService;

    public MemberAuthService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            SocialAccountRegistrationService socialAccountRegistrationService
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.socialAccountRegistrationService = socialAccountRegistrationService;
    }

    public AuthenticatedMember findAuthenticatedMember(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        return AuthenticatedMember.from(user, false);
    }

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
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(socialAccount -> {
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
                        appleRefreshTokenClient
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
            AppleRefreshTokenClient appleRefreshTokenClient
    ) {
        try {
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
