package com.moyeo.auth.apple;

import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleIdentityTokenVerifierTest {

    private static final Instant NOW = Instant.parse("2026-07-23T00:00:00Z");
    private static final String KEY_ID = "apple-key";

    private KeyPair keyPair;
    private RSAKey publicJwk;
    private MockRestServiceServer server;
    private AppleIdentityTokenVerifier verifier;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
        publicJwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.RS256)
                .build();

        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        verifier = new AppleIdentityTokenVerifier(
                restClientBuilder.build(),
                properties(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void verifiesSignatureClaimsAndNonceAndReturnsSubject() throws Exception {
        expectAppleJwks();

        String subject = verifier.verifyAndGetSubject(identityToken("expected-nonce", NOW.plusSeconds(300)), "expected-nonce");

        assertThat(subject).isEqualTo("apple-user-sub");
        server.verify();
    }

    @Test
    void rejectsNonceMismatch() throws Exception {
        expectAppleJwks();

        assertThatThrownBy(() -> verifier.verifyAndGetSubject(
                identityToken("different-nonce", NOW.plusSeconds(300)),
                "expected-nonce"
        )).isInstanceOfSatisfying(MoyeoException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED)
        );
    }

    @Test
    void rejectsExpiredIdentityToken() throws Exception {
        expectAppleJwks();

        assertThatThrownBy(() -> verifier.verifyAndGetSubject(
                identityToken("expected-nonce", NOW.minusSeconds(1)),
                "expected-nonce"
        )).isInstanceOfSatisfying(MoyeoException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED)
        );
    }

    private void expectAppleJwks() {
        server.expect(requestTo("https://appleid.apple.com/auth/keys"))
                .andRespond(withSuccess(
                        new JWKSet(publicJwk).toString(),
                        MediaType.APPLICATION_JSON
                ));
    }

    private String identityToken(String nonce, Instant expiration) throws Exception {
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
                new JWTClaimsSet.Builder()
                        .issuer("https://appleid.apple.com")
                        .audience("com.moyeo.web")
                        .subject("apple-user-sub")
                        .claim("nonce", nonce)
                        .issueTime(Date.from(NOW))
                        .expirationTime(Date.from(expiration))
                        .build()
        );
        signedJwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
        return signedJwt.serialize();
    }

    private AppleOAuthProperties properties() {
        return new AppleOAuthProperties(
                true,
                "com.moyeo.web",
                "TEAM_ID",
                "KEY_ID",
                "unused",
                "https://moyeo-dev.vercel.app/auth/callback/apple",
                "https://appleid.apple.com/auth/token",
                "https://appleid.apple.com/auth/keys",
                Duration.ofSeconds(2),
                Duration.ofSeconds(3),
                Duration.ofHours(1)
        );
    }
}
