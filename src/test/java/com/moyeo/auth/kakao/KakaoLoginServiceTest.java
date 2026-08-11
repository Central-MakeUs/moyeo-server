package com.moyeo.auth.kakao;

import com.moyeo.auth.OAuthRedirectTarget;
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
class KakaoLoginServiceTest {

    @Mock
    private KakaoOAuthClient oauthClient;

    @Mock
    private MemberAuthService memberAuthService;

    @InjectMocks
    private KakaoLoginService kakaoLoginService;

    @Test
    void logsInWithVerifiedKakaoProviderUserId() {
        AuthenticatedMember expected = new AuthenticatedMember(10L, null, true);
        when(oauthClient.exchangeCode("one-time-code", OAuthRedirectTarget.DEV)).thenReturn("access-token");
        when(oauthClient.getProviderUserId("access-token")).thenReturn("1234567890");
        when(memberAuthService.loginSocial(AuthProvider.KAKAO, "1234567890")).thenReturn(expected);

        AuthenticatedMember result = kakaoLoginService.login("one-time-code", OAuthRedirectTarget.DEV);

        assertThat(result).isEqualTo(expected);
        verify(memberAuthService).loginSocial(AuthProvider.KAKAO, "1234567890");
    }

    @Test
    void logsInWithVerifiedKakaoNativeAccessToken() {
        AuthenticatedMember expected = new AuthenticatedMember(10L, null, true);
        when(oauthClient.getProviderUserId("native-access-token")).thenReturn("1234567890");
        when(memberAuthService.loginSocial(AuthProvider.KAKAO, "1234567890")).thenReturn(expected);

        AuthenticatedMember result = kakaoLoginService.loginWithAccessToken("native-access-token");

        assertThat(result).isEqualTo(expected);
        verify(memberAuthService).loginSocial(AuthProvider.KAKAO, "1234567890");
    }

    @Test
    void disconnectsStoredKakaoAccountWithoutReauthentication() {
        kakaoLoginService.disconnectStoredAccount("1234567890");

        verify(oauthClient).unlinkByAdminKey("1234567890");
    }
}
