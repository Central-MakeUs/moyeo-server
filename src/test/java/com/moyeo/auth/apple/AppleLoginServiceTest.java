package com.moyeo.auth.apple;

import com.moyeo.auth.OAuthRedirectTarget;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleLoginServiceTest {

    @Mock
    private AppleTokenClient tokenClient;

    @Mock
    private AppleIdentityTokenVerifier identityTokenVerifier;

    @Mock
    private AppleRefreshTokenCipher refreshTokenCipher;

    @Mock
    private MemberAuthService memberAuthService;

    @InjectMocks
    private AppleLoginService appleLoginService;

    @Test
    void logsInWithVerifiedAppleSubject() {
        AppleTokenClient.AppleTokenResult tokens = tokens();
        AuthenticatedMember expected = new AuthenticatedMember(10L, null, true);
        when(tokenClient.exchange("one-time-code", OAuthRedirectTarget.DEV)).thenReturn(tokens);
        when(identityTokenVerifier.verifyAndGetSubject("identity-token", "nonce"))
                .thenReturn("apple-subject");
        when(refreshTokenCipher.encrypt("apple-subject", "refresh-token"))
                .thenReturn("encrypted-refresh-token");
        when(memberAuthService.loginSocial(
                AuthProvider.APPLE,
                "apple-subject",
                "encrypted-refresh-token",
                AppleRefreshTokenClient.WEB
        )).thenReturn(expected);

        AuthenticatedMember result = appleLoginService.login("one-time-code", "nonce", OAuthRedirectTarget.DEV);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void revokesStoredAppleRefreshTokenWithoutReauthentication() {
        when(refreshTokenCipher.decrypt("apple-subject", "encrypted-refresh-token"))
                .thenReturn("refresh-token");

        appleLoginService.disconnectStoredAuthorization(
                "apple-subject",
                "encrypted-refresh-token"
        );

        verify(tokenClient).revokeRefreshToken("refresh-token", AppleRefreshTokenClient.WEB);
    }

    @Test
    void revokesNativeStoredAppleRefreshTokenWithNativeClient() {
        when(refreshTokenCipher.decrypt("apple-subject", "encrypted-native-refresh-token"))
                .thenReturn("native-refresh-token");

        appleLoginService.disconnectStoredAuthorization(
                "apple-subject",
                "encrypted-native-refresh-token",
                AppleRefreshTokenClient.NATIVE
        );

        verify(tokenClient).revokeRefreshToken("native-refresh-token", AppleRefreshTokenClient.NATIVE);
    }

    @Test
    void logsInWithMatchingVerifiedNativeAppleSubjects() {
        AppleTokenClient.AppleTokenResult tokens = tokens();
        AuthenticatedMember expected = new AuthenticatedMember(11L, null, true);
        when(identityTokenVerifier.verifyNativeAndGetSubject("sdk-identity-token", "native-nonce"))
                .thenReturn("apple-subject");
        when(tokenClient.exchangeNative("native-one-time-code")).thenReturn(tokens);
        when(identityTokenVerifier.verifyNativeAndGetSubject("identity-token", "native-nonce"))
                .thenReturn("apple-subject");
        when(refreshTokenCipher.encrypt("apple-subject", "refresh-token"))
                .thenReturn("encrypted-refresh-token");
        when(memberAuthService.loginSocial(
                AuthProvider.APPLE,
                "apple-subject",
                "encrypted-refresh-token",
                AppleRefreshTokenClient.NATIVE
        ))
                .thenReturn(expected);

        AuthenticatedMember result = appleLoginService.loginNative(
                "sdk-identity-token",
                "native-one-time-code",
                "native-nonce"
        );

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void rejectsNativeLoginWhenSdkAndExchangedSubjectsDiffer() {
        when(identityTokenVerifier.verifyNativeAndGetSubject("sdk-identity-token", "native-nonce"))
                .thenReturn("apple-subject-a");
        when(tokenClient.exchangeNative("native-one-time-code")).thenReturn(tokens());
        when(identityTokenVerifier.verifyNativeAndGetSubject("identity-token", "native-nonce"))
                .thenReturn("apple-subject-b");

        assertThatThrownBy(() -> appleLoginService.loginNative(
                "sdk-identity-token",
                "native-one-time-code",
                "native-nonce"
        )).isInstanceOfSatisfying(com.moyeo.global.error.MoyeoException.class, exception ->
                assertThat(exception.getErrorCode())
                        .isEqualTo(com.moyeo.global.security.AuthenticationErrorCode.SOCIAL_LOGIN_FAILED)
        );
    }

    private AppleTokenClient.AppleTokenResult tokens() {
        return new AppleTokenClient.AppleTokenResult(
                "identity-token",
                "access-token",
                "refresh-token"
        );
    }
}
