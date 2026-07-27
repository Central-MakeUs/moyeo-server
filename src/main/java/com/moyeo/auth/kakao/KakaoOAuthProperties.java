package com.moyeo.auth.kakao;

import com.moyeo.auth.OAuthRedirectTarget;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "moyeo.oauth.kakao")
public record KakaoOAuthProperties(
        boolean enabled,
        String restApiKey,
        String clientSecret,
        String adminKey,
        Map<OAuthRedirectTarget, String> redirectUris,
        String tokenUri,
        String userInfoUri,
        String unlinkUri,
        Duration connectTimeout,
        Duration readTimeout
) {

    void validateWhenEnabled() {
        if (!enabled) {
            return;
        }
        requireText(restApiKey, "KAKAO_OAUTH_REST_API_KEY");
        requireText(clientSecret, "KAKAO_OAUTH_CLIENT_SECRET");
        requireText(adminKey, "KAKAO_OAUTH_ADMIN_KEY");
        requireText(redirectUri(OAuthRedirectTarget.LOCAL), "KAKAO_OAUTH_REDIRECT_URI_LOCAL");
        requireText(redirectUri(OAuthRedirectTarget.DEV), "KAKAO_OAUTH_REDIRECT_URI_DEV");
        requireText(redirectUri(OAuthRedirectTarget.PROD), "KAKAO_OAUTH_REDIRECT_URI_PROD");
        requireText(tokenUri, "Kakao token URI");
        requireText(userInfoUri, "Kakao user information URI");
        requireText(unlinkUri, "Kakao unlink URI");
        if (connectTimeout == null || readTimeout == null) {
            throw new IllegalStateException("Kakao OAuth timeout configuration is required.");
        }
    }

    String redirectUri(OAuthRedirectTarget target) {
        return redirectUris == null ? null : redirectUris.get(target);
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when Kakao OAuth is enabled.");
        }
    }
}
