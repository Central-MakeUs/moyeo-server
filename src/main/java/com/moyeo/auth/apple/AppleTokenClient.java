package com.moyeo.auth.apple;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.auth.OAuthRedirectTarget;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
class AppleTokenClient {

    private static final Logger log = LoggerFactory.getLogger(AppleTokenClient.class);

    private final RestClient restClient;
    private final AppleOAuthProperties properties;
    private final AppleNativeOAuthProperties nativeProperties;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final ObjectMapper objectMapper;

    @Autowired
    AppleTokenClient(
            @Qualifier("appleOAuthRestClient") RestClient restClient,
            AppleOAuthProperties properties,
            AppleNativeOAuthProperties nativeProperties,
            AppleClientSecretGenerator clientSecretGenerator,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.nativeProperties = nativeProperties;
        this.clientSecretGenerator = clientSecretGenerator;
        this.objectMapper = objectMapper;
    }

    AppleTokenClient(
            RestClient restClient,
            AppleOAuthProperties properties,
            AppleClientSecretGenerator clientSecretGenerator,
            ObjectMapper objectMapper
    ) {
        this(restClient, properties, new AppleNativeOAuthProperties(false, null), clientSecretGenerator, objectMapper);
    }

    AppleTokenResult exchange(String code, OAuthRedirectTarget redirectTarget) {
        if (!properties.enabled()) {
            log.warn("Apple login failed: stage=configuration reason=oauth_disabled.");
            throw AppleOAuthException.unavailable();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", clientSecretGenerator.generate());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", properties.redirectUri(redirectTarget));

        return exchange(form);
    }

    AppleTokenResult exchangeNative(String authorizationCode) {
        if (!nativeProperties.enabled()) {
            log.warn("Apple native login failed: stage=configuration reason=oauth_disabled.");
            throw AppleOAuthException.unavailable();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", nativeProperties.clientId());
        form.add("client_secret", clientSecretGenerator.generateForNative());
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");

        AppleTokenResult result = exchange(form);
        if (result.refreshToken() == null || result.refreshToken().isBlank()) {
            log.warn("Apple native login failed: stage=token_exchange providerStatus=200 reason=missing_refresh_token.");
            throw AppleOAuthException.unavailable();
        }
        return result;
    }

    private AppleTokenResult exchange(MultiValueMap<String, String> form) {
        try {
            AppleTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);
            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                log.warn("Apple login failed: stage=token_exchange providerStatus=200 reason=missing_id_token.");
                throw AppleOAuthException.failed();
            }
            return new AppleTokenResult(response.idToken(), response.accessToken(), response.refreshToken());
        } catch (RestClientResponseException exception) {
            String providerError = providerError(exception);
            log.warn(
                    "Apple login failed: stage=token_exchange providerStatus={} providerError={}.",
                    exception.getStatusCode().value(),
                    providerError
            );
            if (exception.getStatusCode().is4xxClientError() && "invalid_grant".equals(providerError)) {
                throw AppleOAuthException.failed();
            }
            throw AppleOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn(
                    "Apple login failed: stage=token_exchange reason=request_failed exception={}.",
                    exception.getClass().getSimpleName()
            );
            throw AppleOAuthException.unavailable();
        }
    }

    AppleTokenResult exchange(String code) {
        return exchange(code, OAuthRedirectTarget.DEV);
    }

    void revokeRefreshToken(String refreshToken, AppleRefreshTokenClient refreshTokenClient) {
        boolean nativeClient = refreshTokenClient == AppleRefreshTokenClient.NATIVE;
        if ((!nativeClient && !properties.enabled()) || (nativeClient && !nativeProperties.enabled())) {
            throw AppleOAuthException.unavailable();
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw AppleOAuthException.unavailable();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", nativeClient ? nativeProperties.clientId() : properties.clientId());
        form.add("client_secret", nativeClient ? clientSecretGenerator.generateForNative() : clientSecretGenerator.generate());
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");

        try {
            restClient.post()
                    .uri(properties.revokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            log.warn("Apple token revocation failed with provider status {}.", exception.getStatusCode().value());
            throw AppleOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn("Apple token revocation request failed: {}", exception.getClass().getSimpleName());
            throw AppleOAuthException.unavailable();
        }
    }

    void revokeRefreshToken(String refreshToken) {
        revokeRefreshToken(refreshToken, AppleRefreshTokenClient.WEB);
    }

    private String providerError(RestClientResponseException exception) {
        try {
            AppleErrorResponse response = objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    AppleErrorResponse.class
            );
            if (response == null) {
                return "unknown";
            }
            return switch (response.error()) {
                case "invalid_request",
                     "invalid_client",
                     "invalid_grant",
                     "unauthorized_client",
                     "unsupported_grant_type",
                     "invalid_scope" -> response.error();
                case null, default -> "unknown";
            };
        } catch (Exception parsingException) {
            return "unknown";
        }
    }

    record AppleTokenResult(String idToken, String accessToken, String refreshToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppleTokenResponse(
            @JsonProperty("id_token") String idToken,
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppleErrorResponse(String error) {
    }
}
