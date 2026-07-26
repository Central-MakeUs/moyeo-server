package com.moyeo.auth.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class KakaoOAuthClientTest {

    private MockRestServiceServer server;
    private KakaoOAuthClient oauthClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        oauthClient = new KakaoOAuthClient(
                restClientBuilder.build(),
                properties(true),
                new ObjectMapper()
        );
    }

    @Test
    void exchangesCodeUsingServerCredentialsAndConfiguredRedirectUri() {
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("grant_type=authorization_code"),
                        org.hamcrest.Matchers.containsString("client_id=kakao-rest-api-key"),
                        org.hamcrest.Matchers.containsString("client_secret=kakao-client-secret"),
                        org.hamcrest.Matchers.containsString(
                                "redirect_uri=https%3A%2F%2Fmoyeo-dev.vercel.app%2Fauth%2Fcallback%2Fkakao"
                        ),
                        org.hamcrest.Matchers.containsString("code=one-time-code")
                )))
                .andRespond(withSuccess(
                        "{\"access_token\":\"kakao-access-token\",\"refresh_token\":\"ignored\"}",
                        MediaType.APPLICATION_JSON
                ));

        assertThat(oauthClient.exchangeCode("one-time-code")).isEqualTo("kakao-access-token");
        server.verify();
    }

    @Test
    void readsOnlyProviderUserIdWithBearerAccessToken() {
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 1234567890,
                          "kakao_account": {
                            "email": "must-not-be-stored@example.com"
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThat(oauthClient.getProviderUserId("kakao-access-token")).isEqualTo("1234567890");
        server.verify();
    }

    @Test
    void unlinksStoredUserWithAdminKey() {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK kakao-admin-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("target_id_type=user_id"),
                        org.hamcrest.Matchers.containsString("target_id=1234567890")
                )))
                .andRespond(withSuccess(
                        "{\"id\":1234567890}",
                        MediaType.APPLICATION_JSON
                ));

        oauthClient.unlinkByAdminKey("1234567890");

        server.verify();
    }

    @Test
    void treatsAlreadyUnlinkedUserAsSuccessful() {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"msg\":\"NotRegisteredUserException\",\"code\":-101}"));

        oauthClient.unlinkByAdminKey("1234567890");

        server.verify();
    }

    @Test
    void mapsUnlinkProviderFailureToSocialLoginUnavailable() {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andRespond(withServerError());

        assertError(
                () -> oauthClient.unlinkByAdminKey("1234567890"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );
        server.verify();
    }

    @Test
    void rejectsUnlinkResponseForDifferentUser() {
        server.expect(requestTo("https://kapi.kakao.com/v1/user/unlink"))
                .andRespond(withSuccess("{\"id\":9999999999}", MediaType.APPLICATION_JSON));

        assertError(
                () -> oauthClient.unlinkByAdminKey("1234567890"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );
        server.verify();
    }

    @Test
    void mapsInvalidGrantToSocialLoginFailed() {
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": "invalid_grant",
                                  "error_description": "authorization code not found",
                                  "error_code": "KOE320"
                                }
                                """));

        assertError(
                () -> oauthClient.exchangeCode("expired-code"),
                AuthenticationErrorCode.SOCIAL_LOGIN_FAILED
        );
    }

    @Test
    void mapsRedirectUriMismatchToSocialLoginUnavailable() {
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": "invalid_grant",
                                  "error_description": "Redirect URI mismatch.",
                                  "error_code": "KOE303"
                                }
                                """));

        assertError(
                () -> oauthClient.exchangeCode("code-issued-for-another-redirect-uri"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );
    }

    @Test
    void recognizesInvalidAuthorizationCodeDescriptionWithoutDetailedErrorCode() {
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "error": "invalid_grant",
                                  "error_description": "authorization code not found for code=expired"
                                }
                                """));

        assertError(
                () -> oauthClient.exchangeCode("expired"),
                AuthenticationErrorCode.SOCIAL_LOGIN_FAILED
        );
    }

    @Test
    void mapsInvalidServerCredentialsToSocialLoginUnavailable() {
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));

        assertError(
                () -> oauthClient.exchangeCode("code"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );
    }

    @Test
    void mapsInvalidAccessTokenToSocialLoginFailed() {
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withUnauthorizedRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "msg": "this access token does not exist",
                                  "code": -401
                                }
                                """));

        assertError(
                () -> oauthClient.getProviderUserId("invalid-access-token"),
                AuthenticationErrorCode.SOCIAL_LOGIN_FAILED
        );
    }

    @Test
    void mapsIpRestrictionFailureToSocialLoginUnavailable() {
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withUnauthorizedRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "msg": "ip mismatched! callerIp=203.0.113.10. check out registered ips.",
                                  "code": -401
                                }
                                """));

        assertError(
                () -> oauthClient.getProviderUserId("access-token"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );
    }

    @Test
    void mapsMalformedOrUnavailableProviderResponseToSocialLoginUnavailable() {
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertError(
                () -> oauthClient.exchangeCode("code"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );

        server.reset();
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withServerError());

        assertError(
                () -> oauthClient.getProviderUserId("access-token"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );
    }

    @Test
    void disabledIntegrationReturnsSocialLoginUnavailableWithoutProviderCall() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer disabledServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        KakaoOAuthClient disabledClient = new KakaoOAuthClient(
                restClientBuilder.build(),
                properties(false),
                new ObjectMapper()
        );

        assertError(
                () -> disabledClient.exchangeCode("code"),
                AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE
        );
        disabledServer.verify();
    }

    private void assertError(Runnable call, AuthenticationErrorCode expectedError) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedError)
                );
    }

    private KakaoOAuthProperties properties(boolean enabled) {
        return new KakaoOAuthProperties(
                enabled,
                "kakao-rest-api-key",
                "kakao-client-secret",
                "kakao-admin-key",
                "https://moyeo-dev.vercel.app/auth/callback/kakao",
                "https://kauth.kakao.com/oauth/token",
                "https://kapi.kakao.com/v2/user/me",
                "https://kapi.kakao.com/v1/user/unlink",
                Duration.ofSeconds(2),
                Duration.ofSeconds(3)
        );
    }
}
