package com.moyeo.auth.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "moyeo.oauth.apple-token-storage")
record AppleRefreshTokenEncryptionProperties(String encryptionKeyBase64) {
}
