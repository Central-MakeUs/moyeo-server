package com.moyeo.global.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String TOKEN_TYPE = "JWT";
    private static final String ALGORITHM = "HS256";
    private static final String ROLE_USER = "USER";
    private static final Instant DEVELOPMENT_TEST_TOKEN_ISSUED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant DEVELOPMENT_TEST_TOKEN_EXPIRES_AT = Instant.parse("2099-01-01T00:00:00Z");

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper) {
        this(jwtProperties, objectMapper, Clock.systemUTC());
    }

    JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String createAccessToken(AuthenticatedMember member) {
        Instant issuedAt = Instant.now(clock);
        return createToken(member, null, issuedAt, issuedAt.plusSeconds(jwtProperties.accessTokenValiditySeconds()));
    }

    public String createAccessToken(AuthenticatedMember member, String sessionId) {
        Instant issuedAt = Instant.now(clock);
        return createToken(member, sessionId, issuedAt, issuedAt.plusSeconds(jwtProperties.accessTokenValiditySeconds()));
    }

    /**
     * Generates the deterministic token used only by the local/dev fixed test accounts.
     * The controller exposing it is profile-restricted; production authentication always
     * uses {@link #createAccessToken(AuthenticatedMember)}.
     */
    public String createDevelopmentTestToken(AuthenticatedMember member) {
        return createToken(member, null, DEVELOPMENT_TEST_TOKEN_ISSUED_AT, DEVELOPMENT_TEST_TOKEN_EXPIRES_AT);
    }

    private String createToken(AuthenticatedMember member, String sessionId, Instant issuedAt, Instant expiresAt) {

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", TOKEN_TYPE);
        header.put("alg", ALGORITHM);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(member.userId()));
        payload.put("role", ROLE_USER);
        if (sessionId != null) {
            payload.put("sid", sessionId);
        }
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public JwtClaims parse(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new IllegalArgumentException("Invalid JWT signature");
        }

        Map<String, Object> header = decodeJson(parts[0]);
        validateHeader(header);

        Map<String, Object> payload = decodeJson(parts[1]);
        long expiresAt = requireNumber(payload, "exp").longValue();
        if (Instant.now(clock).getEpochSecond() >= expiresAt) {
            throw new IllegalArgumentException("Expired JWT");
        }

        requireNumber(payload, "iat");
        String role = requireString(payload, "role");
        if (!ROLE_USER.equals(role)) {
            throw new IllegalArgumentException("Invalid JWT role");
        }

        return new JwtClaims(
                parseUserId(requireString(payload, "sub")),
                role,
                optionalSessionId(payload)
        );
    }

    private void validateHeader(Map<String, Object> header) {
        if (!TOKEN_TYPE.equals(requireString(header, "typ")) || !ALGORITHM.equals(requireString(header, "alg"))) {
            throw new IllegalArgumentException("Invalid JWT header");
        }
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid JWT subject", exception);
        }
    }

    private String requireString(Map<String, Object> value, String key) {
        Object claim = value.get(key);
        if (!(claim instanceof String stringClaim) || stringClaim.isBlank()) {
            throw new IllegalArgumentException("Invalid JWT claim");
        }
        return stringClaim;
    }

    private Number requireNumber(Map<String, Object> value, String key) {
        Object claim = value.get(key);
        if (!(claim instanceof Number numberClaim)) {
            throw new IllegalArgumentException("Invalid JWT claim");
        }
        return numberClaim;
    }

    private String optionalSessionId(Map<String, Object> value) {
        if (!value.containsKey("sid")) {
            return null;
        }
        String sessionId = requireString(value, "sid");
        try {
            java.util.UUID.fromString(sessionId);
            return sessionId;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid JWT session", exception);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to create JWT", exception);
        }
    }

    private Map<String, Object> decodeJson(String encoded) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JWT payload", exception);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }
}
