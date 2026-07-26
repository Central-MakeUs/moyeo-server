package com.moyeo.auth.apple;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(tokenClient.exchange("one-time-code")).thenReturn(tokens);
        when(identityTokenVerifier.verifyAndGetSubject("identity-token", "nonce"))
                .thenReturn("apple-subject");
        when(refreshTokenCipher.encrypt("apple-subject", "refresh-token"))
                .thenReturn("encrypted-refresh-token");
        when(memberAuthService.loginSocial(
                AuthProvider.APPLE,
                "apple-subject",
                "encrypted-refresh-token"
        )).thenReturn(expected);

        AuthenticatedMember result = appleLoginService.login("one-time-code", "nonce");

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

        verify(tokenClient).revokeRefreshToken("refresh-token");
    }

    private AppleTokenClient.AppleTokenResult tokens() {
        return new AppleTokenClient.AppleTokenResult(
                "identity-token",
                "access-token",
                "refresh-token"
        );
    }
}
