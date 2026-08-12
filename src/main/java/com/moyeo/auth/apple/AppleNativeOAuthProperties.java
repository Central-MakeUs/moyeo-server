package com.moyeo.auth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moyeo.oauth.apple.native")
public record AppleNativeOAuthProperties(
        boolean enabled,
        String clientId
) {

    void validateWhenEnabled() {
        if (enabled && (clientId == null || clientId.isBlank())) {
            throw new IllegalStateException("APPLE_NATIVE_CLIENT_ID is required when native Apple OAuth is enabled.");
        }
    }
}
