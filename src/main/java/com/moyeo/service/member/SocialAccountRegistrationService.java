package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.SocialAccount;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SocialAccountRegistrationService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public SocialAccountRegistrationService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository
    ) {
        this.userRepository = userRepository;
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
        User user = userRepository.save(User.pendingOnboarding());
        SocialAccount socialAccount = new SocialAccount(
                user,
                provider,
                providerUserId,
                null,
                providerRefreshTokenCiphertext
        );
        socialAccountRepository.saveAndFlush(socialAccount);
        return AuthenticatedMember.from(user, true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<AuthenticatedMember> findRegistered(AuthProvider provider, String providerUserId) {
        return findRegistered(provider, providerUserId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AuthenticatedMember> findRegistered(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext
    ) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(socialAccount -> {
                    User user = socialAccount.getUser();
                    if (user.getDeletedAt() != null) {
                        throw new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED);
                    }
                    if (providerRefreshTokenCiphertext != null) {
                        socialAccount.updateProviderRefreshTokenCiphertext(providerRefreshTokenCiphertext);
                    }
                    return AuthenticatedMember.from(user, false);
                });
    }
}
