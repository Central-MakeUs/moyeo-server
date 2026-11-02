package com.moyeo.auth.kakao;

import com.moyeo.auth.OAuthRedirectTarget;
import com.moyeo.domain.member.KakaoUnlinkTask;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.KakaoUnlinkTaskRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.service.member.KakaoUnlinkProcessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest(properties = "moyeo.kakao-unlink.scan-delay=24h")
@ActiveProfiles("local")
class KakaoLoginRegistrationRaceTest {
    @Autowired private KakaoLoginService loginService;
    @Autowired private KakaoUnlinkProcessor processor;
    @Autowired private KakaoUnlinkTaskRepository tasks;
    @Autowired private SocialAccountRepository socialAccounts;
    @MockitoBean private KakaoOAuthClient oauthClient;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleanupCompletingAfterFirstProviderLookupCannotRegisterAnInvalidatedToken(boolean nativeLogin) {
        String providerId = nativeLogin ? "native-registration-race" : "web-registration-race";
        KakaoUnlinkTask task = tasks.saveAndFlush(new KakaoUnlinkTask(999999L, providerId));
        if (!nativeLogin) when(oauthClient.exchangeCode("code", OAuthRedirectTarget.DEV)).thenReturn("token");
        when(oauthClient.getProviderUserId("token")).thenAnswer(invocation -> {
            // Identity was valid, but cleanup completes before the service receives the lookup result.
            assertThat(processor.process(task.getId())).isTrue();
            return providerId;
        }).thenThrow(KakaoOAuthException.failed());

        assertThatThrownBy(() -> {
            if (nativeLogin) loginService.loginWithAccessToken("token");
            else loginService.login("code", OAuthRedirectTarget.DEV);
        }).isInstanceOfSatisfying(MoyeoException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED));

        assertThat(tasks.findById(task.getId())).isEmpty();
        assertThat(socialAccounts.findByProviderAndProviderUserId(AuthProvider.KAKAO, providerId)).isEmpty();
        verify(oauthClient, times(2)).getProviderUserId("token");
    }
}
