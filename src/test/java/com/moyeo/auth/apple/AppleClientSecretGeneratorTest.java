package com.moyeo.auth.apple;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AppleClientSecretGeneratorTest {

    @Test
    void generatesSignedAppleClientSecretWithRequiredClaims() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Instant now = Instant.parse("2026-07-23T00:00:00Z");
        AppleOAuthProperties properties = properties(
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
        );

        AppleClientSecretGenerator generator = new AppleClientSecretGenerator(
                properties,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        SignedJWT jwt = SignedJWT.parse(generator.generate());

        assertThat(jwt.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.ES256);
        assertThat(jwt.getHeader().getKeyID()).isEqualTo("KEY_ID");
        assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("TEAM_ID");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("com.moyeo.web");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("https://appleid.apple.com");
        assertThat(jwt.getJWTClaimsSet().getIssueTime().toInstant()).isEqualTo(now);
        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant())
                .isEqualTo(now.plus(Duration.ofMinutes(5)));
    }

    private AppleOAuthProperties properties(String privateKeyBase64) {
        return new AppleOAuthProperties(
                true,
                "com.moyeo.web",
                "TEAM_ID",
                "KEY_ID",
                privateKeyBase64,
                "https://moyeo-dev.vercel.app/auth/callback/apple",
                "https://appleid.apple.com/auth/token",
                "https://appleid.apple.com/auth/keys",
                Duration.ofSeconds(2),
                Duration.ofSeconds(3),
                Duration.ofHours(1)
        );
    }
}
