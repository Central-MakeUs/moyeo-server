package com.moyeo.auth.apple;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final ObjectMapper objectMapper;

    AppleTokenClient(
            @Qualifier("appleOAuthRestClient") RestClient restClient,
            AppleOAuthProperties properties,
            AppleClientSecretGenerator clientSecretGenerator,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.clientSecretGenerator = clientSecretGenerator;
        this.objectMapper = objectMapper;
    }

    String exchange(String code) {
        if (!properties.enabled()) {
            throw AppleOAuthException.unavailable();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", clientSecretGenerator.generate());
        form.add("code", code);
        form.add("grant_type", "authorization_code");
        form.add("redirect_uri", properties.redirectUri());

        try {
            AppleTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);
            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw AppleOAuthException.failed();
            }
            return response.idToken();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                if (isInvalidGrant(exception)) {
                    throw AppleOAuthException.failed();
                }
                log.warn("Apple token exchange rejected the server request with status {}.",
                        exception.getStatusCode().value());
                throw AppleOAuthException.unavailable();
            }
            log.warn("Apple token exchange failed with provider status {}.", exception.getStatusCode().value());
            throw AppleOAuthException.unavailable();
        } catch (RestClientException exception) {
            log.warn("Apple token exchange request failed: {}", exception.getClass().getSimpleName());
            throw AppleOAuthException.unavailable();
        }
    }

    private boolean isInvalidGrant(RestClientResponseException exception) {
        try {
            AppleErrorResponse response = objectMapper.readValue(
                    exception.getResponseBodyAsByteArray(),
                    AppleErrorResponse.class
            );
            return response != null && "invalid_grant".equals(response.error());
        } catch (Exception parsingException) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppleTokenResponse(@JsonProperty("id_token") String idToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppleErrorResponse(String error) {
    }
}
