package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
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
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(socialAccount -> {
                    if (providerRefreshTokenCiphertext != null) {
                        socialAccount.updateProviderRefreshTokenCiphertext(providerRefreshTokenCiphertext);
                    }
                    return authenticatedLoginMember(socialAccount.getUser());
                })
                .orElseGet(() -> registerOrRecoverConcurrentLogin(
                        provider,
                        providerUserId,
                        providerRefreshTokenCiphertext
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
            String providerRefreshTokenCiphertext
    ) {
        try {
            return socialAccountRegistrationService.register(
                    provider,
                    providerUserId,
                    providerRefreshTokenCiphertext
            );
        } catch (DataIntegrityViolationException exception) {
            return socialAccountRegistrationService.findRegistered(
                            provider,
                            providerUserId,
                            providerRefreshTokenCiphertext
                    )
                    .orElseThrow(() -> exception);
        }
    }
}
