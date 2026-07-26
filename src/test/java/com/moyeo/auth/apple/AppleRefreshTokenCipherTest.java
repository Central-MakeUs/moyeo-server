package com.moyeo.auth.apple;

import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleRefreshTokenCipherTest {

    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
    );

    @Test
    void encryptsAndDecryptsRefreshTokenForSameAppleSubject() {
        AppleRefreshTokenCipher cipher = cipher(true, ENCRYPTION_KEY);

        String ciphertext = cipher.encrypt("apple-subject", "apple-refresh-token");

        assertThat(ciphertext).startsWith("v1:");
        assertThat(ciphertext).doesNotContain("apple-refresh-token");
        assertThat(cipher.decrypt("apple-subject", ciphertext))
                .isEqualTo("apple-refresh-token");
    }

    @Test
    void rejectsCiphertextWhenAppleSubjectDoesNotMatch() {
        AppleRefreshTokenCipher cipher = cipher(true, ENCRYPTION_KEY);
        String ciphertext = cipher.encrypt("apple-subject", "apple-refresh-token");

        assertUnavailable(() -> cipher.decrypt("different-subject", ciphertext));
    }

    @Test
    void rejectsInvalidKeyWhenAppleLoginIsEnabled() {
        assertThatThrownBy(() -> cipher(true, "not-a-32-byte-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPLE_REFRESH_TOKEN_ENCRYPTION_KEY_BASE64");
    }

    @Test
    void disabledAppleLoginDoesNotRequireEncryptionKey() {
        AppleRefreshTokenCipher cipher = cipher(false, null);

        assertUnavailable(() -> cipher.encrypt("apple-subject", "refresh-token"));
    }

    private AppleRefreshTokenCipher cipher(boolean enabled, String encryptionKey) {
        return new AppleRefreshTokenCipher(
                new AppleOAuthProperties(
                        enabled,
                        "com.moyeo.web",
                        "TEAM_ID",
                        "KEY_ID",
                        "unused",
                        "https://moyeo.example/auth/apple",
                        "https://appleid.apple.com/auth/token",
                        "https://appleid.apple.com/auth/revoke",
                        "https://appleid.apple.com/auth/keys",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(3),
                        Duration.ofHours(1)
                ),
                new AppleRefreshTokenEncryptionProperties(encryptionKey)
        );
    }

    private void assertUnavailable(Runnable call) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
                );
    }
}
