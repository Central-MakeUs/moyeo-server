package com.moyeo.auth.apple;

import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleTokenClientTest {

    private MockRestServiceServer server;
    private AppleTokenClient tokenClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleClientSecretGenerator clientSecretGenerator = mock(AppleClientSecretGenerator.class);
        when(clientSecretGenerator.generate()).thenReturn("signed-client-secret");
        tokenClient = new AppleTokenClient(
                restClientBuilder.build(),
                properties(),
                clientSecretGenerator
        );
    }

    @Test
    void exchangesCodeUsingServerConfiguredRedirectUri() {
        server.expect(requestTo("https://appleid.apple.com/auth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("client_id=com.moyeo.web"),
                        org.hamcrest.Matchers.containsString("client_secret=signed-client-secret"),
                        org.hamcrest.Matchers.containsString("code=one-time-code"),
                        org.hamcrest.Matchers.containsString("grant_type=authorization_code"),
                        org.hamcrest.Matchers.containsString(
                                "redirect_uri=https%3A%2F%2Fmoyeo-dev.vercel.app%2Fauth%2Fcallback%2Fapple"
                        )
                )))
                .andRespond(withSuccess(
                        "{\"id_token\":\"apple-identity-token\"}",
                        MediaType.APPLICATION_JSON
                ));

        assertThat(tokenClient.exchange("one-time-code")).isEqualTo("apple-identity-token");
        server.verify();
    }

    @Test
    void mapsProviderClientErrorToSocialLoginFailed() {
        server.expect(requestTo("https://appleid.apple.com/auth/token"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> tokenClient.exchange("invalid-code"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED)
                );
    }

    @Test
    void mapsProviderServerErrorToSocialLoginUnavailable() {
        server.expect(requestTo("https://appleid.apple.com/auth/token"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> tokenClient.exchange("code"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
                );
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
