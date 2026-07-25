package com.moyeo.auth.kakao;

import com.moyeo.domain.member.AuthProvider;
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

    public AuthenticatedMember login(String code) {
        String accessToken = oauthClient.exchangeCode(code);
        String providerUserId = oauthClient.getProviderUserId(accessToken);
        return memberAuthService.loginSocial(AuthProvider.KAKAO, providerUserId);
    }
}
