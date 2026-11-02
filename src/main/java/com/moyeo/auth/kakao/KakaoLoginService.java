package com.moyeo.auth.kakao;

import com.moyeo.auth.OAuthRedirectTarget;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import org.springframework.stereotype.Service;

@Service
public class KakaoLoginService {

    private final KakaoOAuthClient oauthClient;
    private final MemberAuthService memberAuthService;

    KakaoLoginService(KakaoOAuthClient oauthClient, MemberAuthService memberAuthService) {
        this.oauthClient = oauthClient;
        this.memberAuthService = memberAuthService;
    }

    public AuthenticatedMember login(String code, OAuthRedirectTarget redirectTarget) {
        String accessToken = oauthClient.exchangeCode(code, redirectTarget);
        String providerUserId = oauthClient.getProviderUserId(accessToken);
        return memberAuthService.loginKakao(providerUserId, () -> {
            if (!providerUserId.equals(oauthClient.getProviderUserId(accessToken))) {
                throw KakaoOAuthException.failed();
            }
        });
    }

    public AuthenticatedMember loginWithAccessToken(String accessToken) {
        String providerUserId = oauthClient.getProviderUserId(accessToken);
        return memberAuthService.loginKakao(providerUserId, () -> {
            if (!providerUserId.equals(oauthClient.getProviderUserId(accessToken))) {
                throw KakaoOAuthException.failed();
            }
        });
    }

    public void disconnectStoredAccount(String providerUserId) {
        oauthClient.unlinkByAdminKey(providerUserId);
    }
}
