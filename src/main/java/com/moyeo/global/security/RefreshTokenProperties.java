package com.moyeo.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moyeo.auth")
public record RefreshTokenProperties(long refreshTokenValiditySeconds) {

    public RefreshTokenProperties {
        if (refreshTokenValiditySeconds <= 0) {
            throw new IllegalArgumentException("Refresh token validity seconds must be positive");
        }
    }
}
