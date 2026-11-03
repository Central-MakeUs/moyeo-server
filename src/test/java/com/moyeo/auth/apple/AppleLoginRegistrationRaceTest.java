package com.moyeo.auth.apple;

import com.moyeo.auth.OAuthRedirectTarget;
import com.moyeo.domain.member.AppleRevokeTask;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.AppleRevokeTaskRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.service.member.AppleRevokeProcessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = "moyeo.apple-revoke.scan-delay=24h")
@ActiveProfiles("local")
class AppleLoginRegistrationRaceTest {
    @Autowired private AppleLoginService login;
    @Autowired private AppleRevokeProcessor processor;
    @Autowired private AppleRevokeTaskRepository tasks;
    @Autowired private SocialAccountRepository accounts;
    @MockitoBean private AppleTokenClient client;
    @MockitoBean private AppleIdentityTokenVerifier verifier;
    @MockitoBean private AppleRefreshTokenCipher cipher;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cleanupCompletingDuringCodeExchangeCannotRegisterRevokedToken(boolean nativeLogin) {
        String sub = nativeLogin ? "apple-native-race" : "apple-web-race";
        AppleRefreshTokenClient tokenClient = nativeLogin ? AppleRefreshTokenClient.NATIVE : AppleRefreshTokenClient.WEB;
        AppleRevokeTask task = tasks.saveAndFlush(new AppleRevokeTask(888888L, sub, "encrypted", tokenClient));
        when(cipher.decrypt(sub, "encrypted")).thenReturn("refresh");
        when(cipher.encrypt(sub, "refresh")).thenReturn("encrypted");
        if (nativeLogin) {
            when(verifier.verifyNativeAndGetSubject("sdk", "nonce")).thenReturn(sub);
            when(verifier.verifyNativeAndGetSubject("id", "nonce")).thenReturn(sub);
            when(client.exchangeNative("code")).thenAnswer(invocation -> {
                assertThat(processor.process(task.getId())).isTrue();
                return new AppleTokenClient.AppleTokenResult("id", "access", "refresh");
            });
        } else {
            when(verifier.verifyAndGetSubject("id", "nonce")).thenReturn(sub);
            when(client.exchange("code", OAuthRedirectTarget.DEV)).thenAnswer(invocation -> {
                assertThat(processor.process(task.getId())).isTrue();
                return new AppleTokenClient.AppleTokenResult("id", "access", "refresh");
            });
        }
        doThrow(AppleOAuthException.failed()).when(client).validateRefreshToken("refresh", tokenClient);

        assertThatThrownBy(() -> {
            if (nativeLogin) login.loginNative("sdk", "code", "nonce");
            else login.login("code", "nonce", OAuthRedirectTarget.DEV);
        }).isInstanceOfSatisfying(MoyeoException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED));
        assertThat(tasks.findById(task.getId())).isEmpty();
        assertThat(accounts.findByProviderAndProviderUserId(AuthProvider.APPLE, sub)).isEmpty();
        verify(client).validateRefreshToken("refresh", tokenClient);
    }
}
