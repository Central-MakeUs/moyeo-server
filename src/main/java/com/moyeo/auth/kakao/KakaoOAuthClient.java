package com.moyeo.auth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Locale;

@Component
class KakaoOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);

    private final RestClient restClient;
    private final KakaoOAuthProperties properties;
    private final ObjectMapper objectMapper;

    KakaoOAuthClient(
            @Qualifier("kakaoOAuthRestClient") RestClient restClient,
            KakaoOAuthProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    String exchangeCode(String code) {
        ensureEnabled();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.restApiKey());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", code);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw KakaoOAuthException.unavailable();
            }
            return response.accessToken();
        } catch (RestClientResponseException exception) {
            if (isInvalidAuthorizationCode(exception)) {
                throw KakaoOAuthException.failed();
            }
            log.warn("Kakao token exchange failed with provider status {}.", exception.getStatusCode().value());
            throw KakaoOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn("Kakao token exchange request failed: {}", exception.getClass().getSimpleName());
            throw KakaoOAuthException.unavailable();
        }
    }

    String getProviderUserId(String accessToken) {
        ensureEnabled();

        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(properties.userInfoUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);
            if (response == null || response.id() == null || response.id() <= 0) {
                throw KakaoOAuthException.unavailable();
            }
            return response.id().toString();
        } catch (RestClientResponseException exception) {
            if (isIpRestrictionFailure(exception)) {
                log.warn("Kakao user information request was rejected by the provider IP restriction.");
                throw KakaoOAuthException.unavailable();
            }
            if (exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw KakaoOAuthException.failed();
            }
            log.warn("Kakao user information request failed with provider status {}.",
                    exception.getStatusCode().value());
            throw KakaoOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn("Kakao user information request failed: {}", exception.getClass().getSimpleName());
            throw KakaoOAuthException.unavailable();
        }
    }

    private void ensureEnabled() {
        if (!properties.enabled()) {
            throw KakaoOAuthException.unavailable();
        }
    }

    private boolean isInvalidAuthorizationCode(RestClientResponseException exception) {
        try {
            KakaoErrorResponse response = objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    KakaoErrorResponse.class
            );
            if (response == null || !"invalid_grant".equals(response.error())) {
                return false;
            }
            return "KOE320".equals(response.errorCode())
                    || containsAuthorizationCodeNotFound(response.errorDescription());
        } catch (Exception parsingException) {
            return false;
        }
    }

    private boolean containsAuthorizationCodeNotFound(String errorDescription) {
        return errorDescription != null
                && errorDescription.toLowerCase(Locale.ROOT).contains("authorization code not found");
    }

    private boolean isIpRestrictionFailure(RestClientResponseException exception) {
        try {
            KakaoErrorResponse response = objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    KakaoErrorResponse.class
            );
            return response != null
                    && Integer.valueOf(-401).equals(response.code())
                    && response.message() != null
                    && response.message().toLowerCase(Locale.ROOT).contains("ip mismatched");
        } catch (Exception parsingException) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUserInfoResponse(Long id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoErrorResponse(
            String error,
            @JsonProperty("error_description") String errorDescription,
            @JsonProperty("error_code") String errorCode,
            Integer code,
            @JsonProperty("msg") String message
    ) {
    }
}
