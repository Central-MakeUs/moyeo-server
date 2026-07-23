package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.SocialAccount;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberAuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    public MemberAuthService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
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
        return socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(socialAccount -> authenticatedLoginMember(socialAccount.getUser()))
                .orElseGet(() -> registerSocial(provider, providerUserId));
    }

    private AuthenticatedMember authenticatedLoginMember(User user) {
        if (user.getDeletedAt() != null) {
            throw new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return AuthenticatedMember.from(user, false);
    }

    private AuthenticatedMember registerSocial(
            AuthProvider provider,
            String providerUserId
    ) {
        User user = userRepository.save(User.pendingOnboarding());
        SocialAccount socialAccount = new SocialAccount(user, provider, providerUserId, null);
        socialAccountRepository.save(socialAccount);
        return AuthenticatedMember.from(user, true);
    }
}
