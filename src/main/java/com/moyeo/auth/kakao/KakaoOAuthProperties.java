package com.moyeo.auth.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "moyeo.oauth.kakao")
public record KakaoOAuthProperties(
        boolean enabled,
        String restApiKey,
        String clientSecret,
        String redirectUri,
        String tokenUri,
        String userInfoUri,
        Duration connectTimeout,
        Duration readTimeout
) {

    void validateWhenEnabled() {
        if (!enabled) {
            return;
        }
        requireText(restApiKey, "KAKAO_OAUTH_REST_API_KEY");
        requireText(clientSecret, "KAKAO_OAUTH_CLIENT_SECRET");
        requireText(redirectUri, "KAKAO_OAUTH_REDIRECT_URI");
        requireText(tokenUri, "Kakao token URI");
        requireText(userInfoUri, "Kakao user information URI");
        if (connectTimeout == null || readTimeout == null) {
            throw new IllegalStateException("Kakao OAuth timeout configuration is required.");
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when Kakao OAuth is enabled.");
        }
    }
}
