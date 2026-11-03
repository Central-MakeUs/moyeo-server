package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.AppleRevokeTaskRepository;
import com.moyeo.repository.member.KakaoUnlinkTaskRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SocialAccountTokenServiceTest {
    @Mock private UserRepository users;
    @Mock private SocialAccountRepository accounts;
    @Mock private KakaoUnlinkTaskRepository kakaoTasks;
    @Mock private AppleRevokeTaskRepository appleTasks;
    private SocialAccountRegistrationService registration;
    private MemberAuthService auth;

    @BeforeEach
    void setUp() {
        registration = new SocialAccountRegistrationService(users, accounts, kakaoTasks, appleTasks);
        auth = new MemberAuthService(users, accounts, registration, kakaoTasks, appleTasks);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void appleLoginRejectsMissingTokenBeforeMembershipReadOrWrite(String token) {
        Runnable verification = mock(Runnable.class);
        assertUnavailable(() -> auth.loginApple("sub", token, AppleRefreshTokenClient.NATIVE, verification));
        assertUnavailable(() -> auth.loginSocial(AuthProvider.APPLE, "sub", token));
        verifyNoInteractions(users, accounts, verification);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void appleRegistrationAndRecoveryRejectMissingTokenBeforeAnyAccountWrite(String token) {
        Runnable verification = mock(Runnable.class);
        assertUnavailable(() -> registration.register(AuthProvider.APPLE, "sub", token));
        assertUnavailable(() -> registration.registerApple("sub", token, AppleRefreshTokenClient.WEB, verification));
        assertUnavailable(() -> registration.findRegistered(AuthProvider.APPLE, "sub", token));
        verifyNoInteractions(users, accounts, verification);
    }

    private void assertUnavailable(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(MoyeoException.class, exception -> {
            assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE);
            assertThat(exception.getErrorCode().status().value()).isEqualTo(503);
        });
    }
}
