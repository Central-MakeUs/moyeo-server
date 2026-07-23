package com.moyeo.auth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "moyeo.oauth.apple")
public record AppleOAuthProperties(
        boolean enabled,
        String clientId,
        String teamId,
        String keyId,
        String privateKeyBase64,
        String redirectUri,
        String tokenUri,
        String jwksUri,
        Duration connectTimeout,
        Duration readTimeout,
        Duration jwksCacheTtl
) {

    void validateWhenEnabled() {
        if (!enabled) {
            return;
        }
        requireText(clientId, "APPLE_CLIENT_ID");
        requireText(teamId, "APPLE_TEAM_ID");
        requireText(keyId, "APPLE_KEY_ID");
        requireText(privateKeyBase64, "APPLE_PRIVATE_KEY_BASE64");
        requireText(redirectUri, "APPLE_REDIRECT_URI");
        requireText(tokenUri, "Apple token URI");
        requireText(jwksUri, "Apple JWKS URI");
        if (connectTimeout == null || readTimeout == null || jwksCacheTtl == null) {
            throw new IllegalStateException("Apple OAuth timeout configuration is required.");
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required when Apple OAuth is enabled.");
        }
    }
}
