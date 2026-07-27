package com.moyeo.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.auth.OAuthRedirectTarget;
import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.controller.TestMemberFactory;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.member.AuthenticatedMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "moyeo.cors.allowed-origins=http://localhost:3000,https://moyeo-web.vercel.app,"
                + "https://moyeo-dev.vercel.app",
        "moyeo.cors.allowed-origin-patterns=https://moyeo-*-hyeonjirohs-projects.vercel.app"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestMemberFactory testMemberFactory;

    @MockitoBean
    private AppleLoginService appleLoginService;

    @MockitoBean
    private KakaoLoginService kakaoLoginService;

    @Test
    void appleLoginReturnsPendingUserAndAccessToken() throws Exception {
        when(appleLoginService.login("apple-code", "nonce", OAuthRedirectTarget.DEV))
                .thenReturn(new AuthenticatedMember(100L, null, true));

        String response = mockMvc.perform(post("/api/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "apple-code",
                                "nonce", "nonce",
                                "redirectTarget", "dev"
                        ))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(100))
                .andExpect(jsonPath("$.user.nickname").doesNotExist())
                .andExpect(jsonPath("$.user.onboardingCompleted").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        assertThat(jwtTokenProvider.parse(authResponse.accessToken()).userId()).isEqualTo(100L);
    }

    @Test
    void appleLoginMapsVerificationFailure() throws Exception {
        when(appleLoginService.login("invalid-code", "nonce", OAuthRedirectTarget.DEV))
                .thenThrow(new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED));

        mockMvc.perform(post("/api/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "invalid-code",
                                "nonce", "nonce",
                                "redirectTarget", "dev"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SOCIAL_LOGIN_FAILED"));
    }

    @Test
    void appleLoginValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "",
                                "nonce", "",
                                "redirectTarget", "dev"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void appleLoginRejectsLocalRedirectTarget() throws Exception {
        mockMvc.perform(post("/api/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "apple-code",
                                "nonce", "nonce",
                                "redirectTarget", "local"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void kakaoLoginReturnsPendingUserAndAccessToken() throws Exception {
        when(kakaoLoginService.login("kakao-code", OAuthRedirectTarget.DEV))
                .thenReturn(new AuthenticatedMember(200L, null, true));

        String response = mockMvc.perform(post("/api/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "kakao-code", "redirectTarget", "dev"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").value(200))
                .andExpect(jsonPath("$.user.nickname").doesNotExist())
                .andExpect(jsonPath("$.user.onboardingCompleted").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse authResponse = objectMapper.readValue(response, AuthResponse.class);
        assertThat(jwtTokenProvider.parse(authResponse.accessToken()).userId()).isEqualTo(200L);
    }

    @Test
    void kakaoLoginMapsVerificationFailure() throws Exception {
        when(kakaoLoginService.login("invalid-code", OAuthRedirectTarget.DEV))
                .thenThrow(new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED));

        mockMvc.perform(post("/api/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "invalid-code", "redirectTarget", "dev"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SOCIAL_LOGIN_FAILED"));
    }

    @Test
    void kakaoLoginValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "", "redirectTarget", "dev"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void kakaoLoginAllowsLocalRedirectTarget() throws Exception {
        when(kakaoLoginService.login("kakao-code", OAuthRedirectTarget.LOCAL))
                .thenReturn(new AuthenticatedMember(200L, null, true));

        mockMvc.perform(post("/api/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "kakao-code",
                                "redirectTarget", "local"
                        ))))
                .andExpect(status().isOk());

        verify(kakaoLoginService).login("kakao-code", OAuthRedirectTarget.LOCAL);
    }

    @Test
    void generalSignupAndLoginEndpointsAreRemoved() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_ENDPOINT_NOT_FOUND"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_ENDPOINT_NOT_FOUND"));
    }

    @Test
    void meReturnsCurrentCompletedUser() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("모여");
        Long userId = jwtTokenProvider.parse(accessToken).userId();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.nickname").value("모여"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    void pendingUserCanReadMeAndCompleteOnboardingIdempotently() throws Exception {
        String accessToken = testMemberFactory.createPendingAccessToken();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").doesNotExist())
                .andExpect(jsonPath("$.onboardingCompleted").value(false));

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(put("/api/users/me/onboarding")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nickname", "모여"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value("모여"))
                    .andExpect(jsonPath("$.onboardingCompleted").value(true));
        }

        mockMvc.perform(put("/api/users/me/onboarding")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "다른 이름"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONBOARDING_ALREADY_COMPLETED"));
    }

    @Test
    void pendingUserCannotCallOtherMemberApis() throws Exception {
        String accessToken = testMemberFactory.createPendingAccessToken();

        mockMvc.perform(get("/api/me/places")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void swaggerDocumentsSocialLoginOnboardingAndSharedOnboardingError() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/auth/apple']['post']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/apple']['post']['responses']['503']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/kakao']['post']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/kakao']['post']['responses']['503']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me/onboarding']['put']['responses']['409']").exists())
                .andExpect(jsonPath("$['paths']['/api/auth/signup']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/auth/login']").doesNotExist())
                .andExpect(jsonPath(
                        "$['paths']['/api/me/places']['get']['responses']['403']['content']['application/problem+json']['example']['code']"
                ).value("ONBOARDING_REQUIRED"))
                .andExpect(jsonPath("$['paths']['/api/auth/me']['get']['responses']['403']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/users/me/onboarding']['put']['responses']['403']").doesNotExist());
    }

    @Test
    void meRequiresValidBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void meRejectsSoftDeletedUserToken() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("deleted");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        jdbcTemplate.update("update users set deleted_at = current_timestamp where id = ?", userId);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:3000",
            "https://moyeo-web.vercel.app",
            "https://moyeo-dev.vercel.app",
            "https://moyeo-pr-check-hyeonjirohs-projects.vercel.app"
    })
    void corsAllowsConfiguredFrontendOrigins(String origin) throws Exception {
        mockMvc.perform(options("/api/auth/apple")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization, Content-Type"));
    }
}
