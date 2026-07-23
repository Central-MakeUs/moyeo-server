package com.moyeo.auth.apple;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Component
class AppleClientSecretGenerator {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final AppleOAuthProperties properties;
    private final Clock clock;
    private final ECPrivateKey privateKey;

    @Autowired
    AppleClientSecretGenerator(AppleOAuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AppleClientSecretGenerator(AppleOAuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        properties.validateWhenEnabled();
        this.privateKey = properties.enabled() ? parsePrivateKey(properties.privateKeyBase64()) : null;
    }

    String generate() {
        if (!properties.enabled() || privateKey == null) {
            throw AppleOAuthException.unavailable();
        }

        Instant issuedAt = Instant.now(clock);
        try {
            SignedJWT clientSecret = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.ES256)
                            .keyID(properties.keyId())
                            .build(),
                    new JWTClaimsSet.Builder()
                            .issuer(properties.teamId())
                            .subject(properties.clientId())
                            .audience(APPLE_ISSUER)
                            .issueTime(Date.from(issuedAt))
                            .expirationTime(Date.from(issuedAt.plus(5, ChronoUnit.MINUTES)))
                            .build()
            );
            clientSecret.sign(new ECDSASigner(privateKey));
            return clientSecret.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign Apple client secret.", exception);
        }
    }

    private ECPrivateKey parsePrivateKey(String encodedKey) {
        try {
            byte[] storedBytes = Base64.getDecoder().decode(encodedKey.strip());
            String decoded = new String(storedBytes, StandardCharsets.US_ASCII);
            byte[] derBytes = decoded.contains("BEGIN PRIVATE KEY")
                    ? decodePem(decoded)
                    : storedBytes;
            return (ECPrivateKey) KeyFactory.getInstance("EC")
                    .generatePrivate(new PKCS8EncodedKeySpec(derBytes));
        } catch (Exception exception) {
            throw new IllegalStateException("APPLE_PRIVATE_KEY_BASE64 is not a valid PKCS#8 EC private key.", exception);
        }
    }

    private byte[] decodePem(String pem) {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
