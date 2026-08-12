package com.moyeo.controller.member;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.controller.TestMemberFactory;
import com.moyeo.controller.meeting.CreateMeetingRequest;
import com.moyeo.controller.meeting.SaveParticipationRequest;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.domain.meeting.TransportationMode;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.AppleRefreshTokenClient;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.meeting.MeetingCoverCleanupProcessor;
import com.moyeo.service.meeting.MeetingCoverStorage;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
import com.moyeo.route.KakaoRouteClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MemberWithdrawalControllerTest {

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

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MeetingCoverCleanupProcessor coverCleanupProcessor;

    @MockitoBean
    private MeetingCoverStorage meetingCoverStorage;

    @MockitoBean
    private AppleLoginService appleLoginService;

    @MockitoBean
    private KakaoLoginService kakaoLoginService;

    @MockitoBean
    private KakaoRouteClient kakaoRouteClient;

    @Test
    void withdrawalDeletesOwnedDataAndHostedMeetingsAndInvalidatesOldToken() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdraw-owner");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        String providerUserId = "withdraw-provider-user";
        prepareMemberOwnedData(userId, providerUserId);

        JsonNode hostedMeeting = createMeeting(accessToken, "withdraw-owned");
        long hostedMeetingId = hostedMeeting.path("meetingId").asLong();
        String coverObjectKey = "meeting-covers/withdraw-owned.jpg";
        jdbcTemplate.update(
                "update meetings set cover_image_key = ? where id = ?",
                coverObjectKey,
                hostedMeetingId
        );

        assertThat(count("social_accounts", "user_id", userId)).isEqualTo(1);
        assertThat(count("saved_places", "user_id", userId)).isEqualTo(1);
        assertThat(count("meeting_schedule_candidates", "meeting_id", hostedMeetingId)).isPositive();
        assertThat(count("meeting_participants", "meeting_id", hostedMeetingId)).isPositive();

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(appleLoginService).disconnectStoredAuthorization(
                providerUserId,
                "encrypted-refresh-token",
                AppleRefreshTokenClient.WEB
        );

        Map<String, Object> withdrawnUser = jdbcTemplate.queryForMap(
                "select nickname, deleted_at from users where id = ?",
                userId
        );
        assertThat(withdrawnUser.get("nickname")).isNull();
        assertThat(withdrawnUser.get("deleted_at")).isNotNull();
        assertThat(count("social_accounts", "user_id", userId)).isZero();
        assertThat(count("saved_places", "user_id", userId)).isZero();
        assertThat(count("meetings", "id", hostedMeetingId)).isZero();
        assertThat(count("meeting_cover_cleanup_tasks", "object_key", coverObjectKey)).isZero();
        verify(meetingCoverStorage).delete(coverObjectKey);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        AuthenticatedMember reRegistered = memberAuthService.loginSocial(
                AuthProvider.APPLE,
                providerUserId,
                "new-encrypted-refresh-token"
        );
        assertThat(reRegistered.userId()).isNotEqualTo(userId);
        assertThat(reRegistered.onboardingCompleted()).isFalse();
    }

    @Test
    void failedCoverDeletionRemainsRetryableAfterWithdrawal() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdraw-cover-retry");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        String providerUserId = "withdraw-cover-retry-provider";
        insertSocialAccount(userId, AuthProvider.KAKAO, providerUserId);
        JsonNode hostedMeeting = createMeeting(accessToken, "withdraw-retry");
        long hostedMeetingId = hostedMeeting.path("meetingId").asLong();
        String coverObjectKey = "meeting-covers/withdraw-retry.jpg";
        jdbcTemplate.update(
                "update meetings set cover_image_key = ? where id = ?",
                coverObjectKey,
                hostedMeetingId
        );
        doThrow(new IllegalStateException("temporary storage failure"))
                .doNothing()
                .when(meetingCoverStorage)
                .delete(coverObjectKey);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());

        assertThat(count("meetings", "id", hostedMeetingId)).isZero();
        assertThat(count("meeting_cover_cleanup_tasks", "object_key", coverObjectKey)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select attempt_count from meeting_cover_cleanup_tasks where object_key = ?",
                Integer.class,
                coverObjectKey
        )).isEqualTo(1);

        coverCleanupProcessor.processPending();

        assertThat(count("meeting_cover_cleanup_tasks", "object_key", coverObjectKey)).isZero();
        verify(meetingCoverStorage, times(2)).delete(coverObjectKey);
    }

    @Test
    void withdrawalHardDeletesNonHostParticipationAndParticipationData() throws Exception {
        String hostAccessToken = testMemberFactory.createAccessToken("withdraw-nonhost-owner");
        JsonNode hostedMeeting = createMeeting(hostAccessToken, "withdraw-nhost");
        long meetingId = hostedMeeting.path("meetingId").asLong();
        String inviteCode = hostedMeeting.path("inviteCode").asText();

        String memberAccessToken = testMemberFactory.createAccessToken("withdraw-nonhost-member");
        Long memberUserId = jwtTokenProvider.parse(memberAccessToken).userId();
        insertSocialAccount(memberUserId, AuthProvider.KAKAO, "withdraw-nonhost-provider");
        LocalDate candidateDate = LocalDate.now().plusDays(1);
        String joinResponse = mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", bearer(memberAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "withdraw",
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", candidateDate.toString(),
                                                "startTime", "09:00",
                                                "endTime", "10:00"
                                        ))
                                ),
                                "departure", Map.of(
                                        "name", "member-home",
                                        "address", "서울 강남구",
                                        "latitude", 37.5,
                                        "longitude", 127.0,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long participantId = objectMapper.readTree(joinResponse).path("participantId").asLong();

        assertThat(count("meeting_participants", "id", participantId)).isEqualTo(1);
        assertThat(count("meeting_participant_schedule_availabilities", "participant_id", participantId))
                .isEqualTo(1);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(memberAccessToken)))
                .andExpect(status().isNoContent());

        assertThat(count("meetings", "id", meetingId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select max_participants from meetings where id = ?", Integer.class, meetingId))
                .isEqualTo(5);
        assertThat(count("meeting_participants", "meeting_id", meetingId)).isEqualTo(1);
        assertThat(count("meeting_participants", "id", participantId)).isZero();
        assertThat(count("meeting_participant_schedule_availabilities", "participant_id", participantId))
                .isZero();
    }

    @Test
    void withdrawalFromFullMeetingDeletesActualTimeSnapshotAndAllowsRegeneration() throws Exception {
        String hostAccessToken = testMemberFactory.createAccessToken("withdraw-snapshot-host");
        JsonNode hostedMeeting = createMeeting(hostAccessToken, "snapshot-out");
        long meetingId = hostedMeeting.path("meetingId").asLong();
        String inviteCode = hostedMeeting.path("inviteCode").asText();

        String memberAccessToken = testMemberFactory.createAccessToken("withdraw-snapshot-member");
        Long memberUserId = jwtTokenProvider.parse(memberAccessToken).userId();
        insertSocialAccount(memberUserId, AuthProvider.KAKAO, "withdraw-snapshot-provider");
        joinMember(inviteCode, memberAccessToken, "wdsnap");
        jdbcTemplate.update("update meetings set max_participants = 2 where id = ?", meetingId);
        org.mockito.Mockito.when(kakaoRouteClient.findShortestTravelTimeSeconds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(1_200L);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationBasis").value("ACTUAL_TRAVEL_TIME"));
        assertThat(count("meeting_place_recommendation_snapshots", "meeting_id", meetingId)).isEqualTo(3);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(memberAccessToken)))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("select max_participants from meetings where id = ?", Integer.class, meetingId))
                .isEqualTo(1);
        assertThat(count("meeting_place_recommendation_snapshots", "meeting_id", meetingId)).isZero();

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationBasis").value("ACTUAL_TRAVEL_TIME"))
                .andExpect(jsonPath("$.recommendations[0].averageTravelTimeSeconds").value(1200));
        verify(kakaoRouteClient, times(9)).findShortestTravelTimeSeconds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void pendingOnboardingUserCanWithdraw() throws Exception {
        String accessToken = testMemberFactory.createPendingAccessToken();
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        String providerUserId = "withdraw-pending-apple";
        insertSocialAccount(userId, AuthProvider.APPLE, providerUserId);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is not null from users where id = ?",
                Boolean.class,
                userId
        )).isTrue();
    }

    @Test
    void fixedDevelopmentTestAccountCanWithdrawWithoutSocialReauthentication() throws Exception {
        Long userId = jdbcTemplate.queryForObject(
                "select id from users where nickname = '슈퍼토큰유저' and deleted_at is null order by id limit 1",
                Long.class
        );
        String accessToken = jwtTokenProvider.createAccessToken(
                new AuthenticatedMember(userId, "슈퍼토큰유저", false)
        );

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is not null from users where id = ?",
                Boolean.class,
                userId
        )).isTrue();
    }

    @Test
    void nonFixedUserWithoutSocialAccountCannotBypassSocialAccountRequirement() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("not-a-fixed-development-user");
        Long userId = jwtTokenProvider.parse(accessToken).userId();

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_SERVER_ERROR"));

        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is null from users where id = ?",
                Boolean.class,
                userId
        )).isTrue();
    }

    @Test
    void kakaoUserCanRegisterAsNewUserAfterWithdrawal() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdraw-kakao");
        Long withdrawnUserId = jwtTokenProvider.parse(accessToken).userId();
        String providerUserId = "1234567890";
        jdbcTemplate.update(
                """
                insert into social_accounts(
                    user_id, provider, provider_user_id, email,
                    provider_refresh_token_ciphertext, created_at
                )
                values (?, 'KAKAO', ?, null, null, current_timestamp)
                """,
                withdrawnUserId,
                providerUserId
        );

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());

        verify(kakaoLoginService).disconnectStoredAccount(providerUserId);
        AuthenticatedMember reRegistered = memberAuthService.loginSocial(AuthProvider.KAKAO, providerUserId);
        assertThat(reRegistered.userId()).isNotEqualTo(withdrawnUserId);
        assertThat(reRegistered.onboardingCompleted()).isFalse();
        assertThat(reRegistered.registered()).isTrue();
    }

    @Test
    void withdrawalRequiresAuthenticationAndIsDocumentedInSwagger() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['requestBody']").doesNotExist())
                .andExpect(jsonPath("$['components']['schemas']['WithdrawMemberRequest']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['400']").doesNotExist())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['500']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['503']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['403']").doesNotExist());
    }

    @Test
    void providerDisconnectFailureKeepsLocalAccountAndSocialLink() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdraw-provider-failure");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        String providerUserId = "withdraw-provider-failure-sub";
        prepareMemberOwnedData(userId, providerUserId);
        JsonNode hostedMeeting = createMeeting(accessToken, "withdraw-fail");
        long hostedMeetingId = hostedMeeting.path("meetingId").asLong();
        String coverObjectKey = "meeting-covers/withdraw-provider-failure.jpg";
        jdbcTemplate.update(
                "update meetings set cover_image_key = ? where id = ?",
                coverObjectKey,
                hostedMeetingId
        );
        doThrow(new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE))
                .when(appleLoginService)
                .disconnectStoredAuthorization(providerUserId, "encrypted-refresh-token", AppleRefreshTokenClient.WEB);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SOCIAL_LOGIN_UNAVAILABLE"));

        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is null from users where id = ?",
                Boolean.class,
                userId
        )).isTrue();
        assertThat(count("social_accounts", "user_id", userId)).isEqualTo(1);
        assertThat(count("saved_places", "user_id", userId)).isEqualTo(1);
        assertThat(count("meetings", "id", hostedMeetingId)).isEqualTo(1);
        assertThat(count("meeting_participants", "meeting_id", hostedMeetingId)).isPositive();
        assertThat(count("meeting_schedule_candidates", "meeting_id", hostedMeetingId)).isPositive();
        assertThat(count("meeting_cover_cleanup_tasks", "object_key", coverObjectKey)).isZero();
    }

    @Test
    void appleWithdrawalRequiresStoredRefreshTokenAndKeepsLocalAccountWhenMissing() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdraw-missing-refresh-token");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        jdbcTemplate.update(
                """
                insert into social_accounts(user_id, provider, provider_user_id, email, created_at)
                values (?, 'APPLE', 'withdraw-missing-token-sub', null, current_timestamp)
                """,
                userId
        );

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SOCIAL_LOGIN_UNAVAILABLE"));

        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is null from users where id = ?",
                Boolean.class,
                userId
        )).isTrue();
        assertThat(count("social_accounts", "user_id", userId)).isEqualTo(1);
    }

    private void prepareMemberOwnedData(Long userId, String providerUserId) {
        jdbcTemplate.update(
                """
                insert into social_accounts(
                    user_id, provider, provider_user_id, email,
                    provider_refresh_token_ciphertext, created_at
                )
                values (?, 'APPLE', ?, null, ?, current_timestamp)
                """,
                userId,
                providerUserId,
                "encrypted-refresh-token"
        );
        jdbcTemplate.update(
                """
                insert into saved_places(
                    user_id, alias, category, type, display_name, address,
                    latitude, longitude, created_at, updated_at
                )
                values (?, '회사', 'OTHER', 'PLACE', '회사', '서울', 37.5, 127.0, current_timestamp, current_timestamp)
                """,
                userId
        );
    }

    private JsonNode createMeeting(String accessToken, String name) throws Exception {
        LocalDate candidateDate = LocalDate.now().plusDays(1);
        CreateMeetingRequest request = new CreateMeetingRequest(
                name,
                null,
                6,
                PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                List.of(candidateDate),
                new SaveParticipationRequest.ScheduleResponseRequest(
                        null,
                        List.of(new SaveParticipationRequest.ScheduleAvailabilityRequest(
                                candidateDate,
                                LocalTime.of(9, 0),
                                LocalTime.of(10, 0)
                        ))
                ),
                new SaveParticipationRequest.DepartureRequest(
                        "회사",
                        "서울",
                        new BigDecimal("37.5"),
                        new BigDecimal("127.0"),
                        TransportationMode.PUBLIC_TRANSIT
                ),
                1440
        );
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private void joinMember(String inviteCode, String accessToken, String nickname) throws Exception {
        LocalDate candidateDate = LocalDate.now().plusDays(1);
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", nickname,
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", candidateDate.toString(),
                                                "startTime", "09:00",
                                                "endTime", "10:00"
                                        ))
                                ),
                                "departure", Map.of(
                                        "name", "member-home",
                                        "address", "서울 강남구",
                                        "latitude", 37.5,
                                        "longitude", 127.0,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isCreated());
    }

    private long count(String table, String column, Object value) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + column + " = ?",
                Long.class,
                value
        );
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private void insertSocialAccount(Long userId, AuthProvider provider, String providerUserId) {
        jdbcTemplate.update(
                """
                insert into social_accounts(
                    user_id, provider, provider_user_id, email,
                    provider_refresh_token_ciphertext, created_at
                )
                values (?, ?, ?, null, ?, current_timestamp)
                """,
                userId,
                provider.name(),
                providerUserId,
                provider == AuthProvider.APPLE ? "encrypted-refresh-token" : null
        );
    }
}
