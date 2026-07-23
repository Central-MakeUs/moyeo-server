package com.moyeo.auth.apple;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import org.springframework.stereotype.Service;

@Service
public class AppleLoginService {

    private final AppleTokenClient tokenClient;
    private final AppleIdentityTokenVerifier identityTokenVerifier;
    private final MemberAuthService memberAuthService;

    AppleLoginService(
            AppleTokenClient tokenClient,
            AppleIdentityTokenVerifier identityTokenVerifier,
            MemberAuthService memberAuthService
    ) {
        this.tokenClient = tokenClient;
        this.identityTokenVerifier = identityTokenVerifier;
        this.memberAuthService = memberAuthService;
    }

    public AuthenticatedMember login(String code, String nonce) {
        String identityToken = tokenClient.exchange(code);
        String subject = identityTokenVerifier.verifyAndGetSubject(identityToken, nonce);
        return memberAuthService.loginSocial(AuthProvider.APPLE, subject);
    }
}
