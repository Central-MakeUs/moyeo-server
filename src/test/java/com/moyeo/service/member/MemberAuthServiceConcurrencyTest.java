package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberAuthServiceConcurrencyTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private SocialAccountRegistrationService socialAccountRegistrationService;

    @InjectMocks
    private MemberAuthService memberAuthService;

    @Test
    void recoversConcurrentFirstLoginByReturningAlreadyRegisteredAccount() {
        when(socialAccountRepository.findByProviderAndProviderUserId(AuthProvider.APPLE, "apple-user"))
                .thenReturn(Optional.empty());
        when(socialAccountRegistrationService.register(AuthProvider.APPLE, "apple-user", null))
                .thenThrow(new DataIntegrityViolationException("duplicate provider identity"));
        when(socialAccountRegistrationService.findRegistered(AuthProvider.APPLE, "apple-user", null))
                .thenReturn(Optional.of(new AuthenticatedMember(7L, null, false)));

        AuthenticatedMember result = memberAuthService.loginSocial(AuthProvider.APPLE, "apple-user");

        assertThat(result.userId()).isEqualTo(7L);
        assertThat(result.registered()).isFalse();
    }
}
