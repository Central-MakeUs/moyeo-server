package com.moyeo.auth.apple;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
class AppleIdentityTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(AppleIdentityTokenVerifier.class);
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final RestClient restClient;
    private final AppleOAuthProperties properties;
    private final Clock clock;

    private volatile CachedJwkSet cachedJwkSet;

    @Autowired
    AppleIdentityTokenVerifier(
            @Qualifier("appleOAuthRestClient") RestClient restClient,
            AppleOAuthProperties properties
    ) {
        this(restClient, properties, Clock.systemUTC());
    }

    AppleIdentityTokenVerifier(RestClient restClient, AppleOAuthProperties properties, Clock clock) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
    }

    String verifyAndGetSubject(String identityToken, String expectedNonce) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(identityToken);
            if (!JWSAlgorithm.RS256.equals(signedJwt.getHeader().getAlgorithm())) {
                throw AppleOAuthException.failed();
            }

            RSAKey rsaKey = findRsaKey(signedJwt.getHeader().getKeyID());
            if (!signedJwt.verify(new RSASSAVerifier(rsaKey))) {
                throw AppleOAuthException.failed();
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            validateClaims(claims, expectedNonce);
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw AppleOAuthException.failed();
            }
            return subject;
        } catch (com.moyeo.global.error.MoyeoException exception) {
            throw exception;
        } catch (Exception exception) {
            throw AppleOAuthException.failed();
        }
    }

    private void validateClaims(JWTClaimsSet claims, String expectedNonce) throws java.text.ParseException {
        Date expirationTime = claims.getExpirationTime();
        if (!APPLE_ISSUER.equals(claims.getIssuer())
                || !claims.getAudience().contains(properties.clientId())
                || expirationTime == null
                || !expirationTime.toInstant().isAfter(Instant.now(clock))
                || !expectedNonce.equals(claims.getStringClaim("nonce"))) {
            throw AppleOAuthException.failed();
        }
    }

    private RSAKey findRsaKey(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw AppleOAuthException.failed();
        }

        CachedJwkSet current = cachedJwkSet;
        if (current != null && current.isFresh(Instant.now(clock), properties.jwksCacheTtl())) {
            RSAKey key = rsaKey(current.jwkSet(), keyId);
            if (key != null) {
                return key;
            }
        }
        return refreshAndFind(keyId);
    }

    private synchronized RSAKey refreshAndFind(String keyId) {
        CachedJwkSet current = cachedJwkSet;
        if (current != null && current.isFresh(Instant.now(clock), properties.jwksCacheTtl())) {
            RSAKey existing = rsaKey(current.jwkSet(), keyId);
            if (existing != null) {
                return existing;
            }
        }

        try {
            String response = restClient.get()
                    .uri(properties.jwksUri())
                    .retrieve()
                    .body(String.class);
            if (response == null || response.isBlank()) {
                throw AppleOAuthException.unavailable();
            }
            JWKSet jwkSet = JWKSet.parse(response);
            cachedJwkSet = new CachedJwkSet(jwkSet, Instant.now(clock));
            RSAKey refreshed = rsaKey(jwkSet, keyId);
            if (refreshed == null) {
                throw AppleOAuthException.failed();
            }
            return refreshed;
        } catch (com.moyeo.global.error.MoyeoException exception) {
            throw exception;
        } catch (RestClientException exception) {
            log.warn("Apple public key request failed: {}", exception.getClass().getSimpleName());
            throw AppleOAuthException.unavailable();
        } catch (Exception exception) {
            log.warn("Apple public key response could not be parsed.");
            throw AppleOAuthException.unavailable();
        }
    }

    private RSAKey rsaKey(JWKSet jwkSet, String keyId) {
        JWK key = jwkSet.getKeyByKeyId(keyId);
        return key instanceof RSAKey rsaKey ? rsaKey : null;
    }

    private record CachedJwkSet(JWKSet jwkSet, Instant fetchedAt) {

        boolean isFresh(Instant now, java.time.Duration ttl) {
            return now.isBefore(fetchedAt.plus(ttl));
        }
    }
}
