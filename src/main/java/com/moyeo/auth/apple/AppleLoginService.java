package com.moyeo.auth.apple;

import com.moyeo.auth.OAuthRedirectTarget;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import org.springframework.stereotype.Service;

@Service
public class AppleLoginService {

    private final AppleTokenClient tokenClient;
    private final AppleIdentityTokenVerifier identityTokenVerifier;
    private final AppleRefreshTokenCipher refreshTokenCipher;
    private final MemberAuthService memberAuthService;

    AppleLoginService(
            AppleTokenClient tokenClient,
            AppleIdentityTokenVerifier identityTokenVerifier,
            AppleRefreshTokenCipher refreshTokenCipher,
            MemberAuthService memberAuthService
    ) {
        this.tokenClient = tokenClient;
        this.identityTokenVerifier = identityTokenVerifier;
        this.refreshTokenCipher = refreshTokenCipher;
        this.memberAuthService = memberAuthService;
    }

    public AuthenticatedMember login(String code, String nonce, OAuthRedirectTarget redirectTarget) {
        AppleTokenClient.AppleTokenResult tokens = tokenClient.exchange(code, redirectTarget);
        String subject = identityTokenVerifier.verifyAndGetSubject(tokens.idToken(), nonce);
        String encryptedRefreshToken = refreshTokenCipher.encrypt(subject, tokens.refreshToken());
        return memberAuthService.loginSocial(AuthProvider.APPLE, subject, encryptedRefreshToken);
    }

    public void disconnectStoredAuthorization(
            String expectedSubject,
            String encryptedRefreshToken
    ) {
        String refreshToken = refreshTokenCipher.decrypt(expectedSubject, encryptedRefreshToken);
        tokenClient.revokeRefreshToken(refreshToken);
    }
}
