package com.moyeo.auth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.auth.OAuthRedirectTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
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

    String exchangeCode(String code, OAuthRedirectTarget redirectTarget) {
        ensureEnabled();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.restApiKey());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri(redirectTarget));
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
            KakaoErrorResponse providerError = parseErrorResponse(exception);
            if (isInvalidAuthorizationCode(providerError)) {
                throw KakaoOAuthException.failed();
            }
            logProviderFailure("token exchange", exception, providerError);
            throw KakaoOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn("Kakao token exchange request failed: {}", exception.getClass().getSimpleName());
            throw KakaoOAuthException.unavailable();
        }
    }

    String exchangeCode(String code) {
        return exchangeCode(code, OAuthRedirectTarget.DEV);
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
            KakaoErrorResponse providerError = parseErrorResponse(exception);
            if (isIpRestrictionFailure(providerError)) {
                log.warn(
                        "Kakao user information request was rejected by the provider IP restriction: "
                                + "status={}, providerCode={}.",
                        exception.getStatusCode().value(),
                        providerError.code()
                );
                throw KakaoOAuthException.unavailable();
            }
            if (exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw KakaoOAuthException.failed();
            }
            logProviderFailure("user information request", exception, providerError);
            throw KakaoOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn("Kakao user information request failed: {}", exception.getClass().getSimpleName());
            throw KakaoOAuthException.unavailable();
        }
    }

    void unlinkByAdminKey(String expectedProviderUserId) {
        ensureEnabled();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", expectedProviderUserId);

        try {
            KakaoUnlinkResponse response = restClient.post()
                    .uri(properties.unlinkUri())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.adminKey())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoUnlinkResponse.class);
            if (response == null
                    || response.id() == null
                    || !expectedProviderUserId.equals(response.id().toString())) {
                throw KakaoOAuthException.unavailable();
            }
        } catch (RestClientResponseException exception) {
            KakaoErrorResponse providerError = parseErrorResponse(exception);
            if (isAlreadyUnlinked(providerError)) {
                return;
            }
            logProviderFailure("unlink request", exception, providerError);
            throw KakaoOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn("Kakao unlink request failed: {}", exception.getClass().getSimpleName());
            throw KakaoOAuthException.unavailable();
        }
    }

    private void ensureEnabled() {
        if (!properties.enabled()) {
            throw KakaoOAuthException.unavailable();
        }
    }

    private boolean isInvalidAuthorizationCode(KakaoErrorResponse response) {
        if (response == null || !"invalid_grant".equals(response.error())) {
            return false;
        }
        return "KOE320".equals(response.errorCode())
                || containsAuthorizationCodeNotFound(response.errorDescription());
    }

    private boolean containsAuthorizationCodeNotFound(String errorDescription) {
        return errorDescription != null
                && errorDescription.toLowerCase(Locale.ROOT).contains("authorization code not found");
    }

    private boolean isIpRestrictionFailure(KakaoErrorResponse response) {
        return response != null
                && Integer.valueOf(-401).equals(response.code())
                && response.message() != null
                && response.message().toLowerCase(Locale.ROOT).contains("ip mismatched");
    }

    private boolean isAlreadyUnlinked(KakaoErrorResponse response) {
        return response != null && Integer.valueOf(-101).equals(response.code());
    }

    private KakaoErrorResponse parseErrorResponse(RestClientResponseException exception) {
        try {
            return objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    KakaoErrorResponse.class
            );
        } catch (Exception parsingException) {
            return null;
        }
    }

    private void logProviderFailure(
            String operation,
            RestClientResponseException exception,
            KakaoErrorResponse providerError
    ) {
        log.warn(
                "Kakao {} failed: status={}, providerError={}, providerErrorCode={}, providerCode={}.",
                operation,
                exception.getStatusCode().value(),
                providerError == null ? null : providerError.error(),
                providerError == null ? null : providerError.errorCode(),
                providerError == null ? null : providerError.code()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUserInfoResponse(Long id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUnlinkResponse(Long id) {
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
