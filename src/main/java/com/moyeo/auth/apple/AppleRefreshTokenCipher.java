package com.moyeo.auth.apple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
class AppleRefreshTokenCipher {

    private static final String VERSION_PREFIX = "v1:";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey key;
    private final SecureRandom secureRandom;

    @Autowired
    AppleRefreshTokenCipher(
            AppleOAuthProperties oauthProperties,
            AppleRefreshTokenEncryptionProperties encryptionProperties
    ) {
        this(oauthProperties, encryptionProperties, new SecureRandom());
    }

    AppleRefreshTokenCipher(
            AppleOAuthProperties oauthProperties,
            AppleRefreshTokenEncryptionProperties encryptionProperties,
            SecureRandom secureRandom
    ) {
        this.secureRandom = secureRandom;
        this.key = oauthProperties.enabled()
                ? decodeKey(encryptionProperties.encryptionKeyBase64())
                : null;
    }

    String encrypt(String providerUserId, String refreshToken) {
        requireAvailable();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AppleOAuthException.unavailable();
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad(providerUserId));
            byte[] ciphertext = cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            return VERSION_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception exception) {
            throw AppleOAuthException.unavailable();
        }
    }

    String decrypt(String providerUserId, String storedCiphertext) {
        requireAvailable();
        if (storedCiphertext == null || !storedCiphertext.startsWith(VERSION_PREFIX)) {
            throw AppleOAuthException.unavailable();
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(
                    storedCiphertext.substring(VERSION_PREFIX.length())
            );
            if (payload.length <= IV_LENGTH_BYTES) {
                throw AppleOAuthException.unavailable();
            }
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad(providerUserId));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (com.moyeo.global.error.MoyeoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw AppleOAuthException.unavailable();
        }
    }

    private SecretKey decodeKey(String encodedKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey.strip());
            if (decoded.length != KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "APPLE_REFRESH_TOKEN_ENCRYPTION_KEY_BASE64 must decode to 32 bytes."
                );
            }
            return new SecretKeySpec(decoded, "AES");
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "APPLE_REFRESH_TOKEN_ENCRYPTION_KEY_BASE64 must be valid Base64 for 32 bytes.",
                    exception
            );
        }
    }

    private byte[] aad(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw AppleOAuthException.unavailable();
        }
        return ("APPLE\u0000" + providerUserId).getBytes(StandardCharsets.UTF_8);
    }

    private void requireAvailable() {
        if (key == null) {
            throw AppleOAuthException.unavailable();
        }
    }
}
