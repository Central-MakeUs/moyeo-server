package com.moyeo.controller.member;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.controller.TestMemberFactory;
import com.moyeo.controller.meeting.CreateMeetingRequest;
import com.moyeo.controller.meeting.SaveParticipationRequest;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.domain.meeting.TransportationMode;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.meeting.MeetingCoverCleanupProcessor;
import com.moyeo.service.meeting.MeetingCoverStorage;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberAuthService;
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
        long meetingSearchId = insertDepartureSearch(null, hostedMeetingId, "withdraw-meeting-search");
        insertDepartureSearchCandidate(meetingSearchId);

        assertThat(count("social_accounts", "user_id", userId)).isEqualTo(1);
        assertThat(count("saved_places", "user_id", userId)).isEqualTo(1);
        assertThat(count("departure_place_searches", "user_id", userId)).isEqualTo(1);
        assertThat(count("meeting_schedule_candidates", "meeting_id", hostedMeetingId)).isPositive();
        assertThat(count("meeting_participants", "meeting_id", hostedMeetingId)).isPositive();

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Map<String, Object> withdrawnUser = jdbcTemplate.queryForMap(
                "select nickname, deleted_at from users where id = ?",
                userId
        );
        assertThat(withdrawnUser.get("nickname")).isNull();
        assertThat(withdrawnUser.get("deleted_at")).isNotNull();
        assertThat(count("social_accounts", "user_id", userId)).isZero();
        assertThat(count("saved_places", "user_id", userId)).isZero();
        assertThat(count("departure_place_searches", "user_id", userId)).isZero();
        assertThat(count("meetings", "id", hostedMeetingId)).isZero();
        assertThat(count("departure_place_searches", "meeting_id", hostedMeetingId)).isZero();
        assertThat(count("meeting_cover_cleanup_tasks", "object_key", coverObjectKey)).isZero();
        verify(meetingCoverStorage).delete(coverObjectKey);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        AuthenticatedMember reRegistered = memberAuthService.loginSocial(AuthProvider.APPLE, providerUserId);
        assertThat(reRegistered.userId()).isNotEqualTo(userId);
        assertThat(reRegistered.onboardingCompleted()).isFalse();
    }

    @Test
    void failedCoverDeletionRemainsRetryableAfterWithdrawal() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdraw-cover-retry");
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
    void pendingOnboardingUserCanWithdraw() throws Exception {
        String accessToken = testMemberFactory.createPendingAccessToken();
        Long userId = jwtTokenProvider.parse(accessToken).userId();

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
    void kakaoUserCanRegisterAsNewUserAfterWithdrawal() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdraw-kakao");
        Long withdrawnUserId = jwtTokenProvider.parse(accessToken).userId();
        String providerUserId = "1234567890";
        jdbcTemplate.update(
                """
                insert into social_accounts(user_id, provider, provider_user_id, email, created_at)
                values (?, 'KAKAO', ?, null, current_timestamp)
                """,
                withdrawnUserId,
                providerUserId
        );

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());

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
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['204']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['401']").exists())
                .andExpect(jsonPath("$['paths']['/api/users/me']['delete']['responses']['403']").doesNotExist());
    }

    private void prepareMemberOwnedData(Long userId, String providerUserId) {
        jdbcTemplate.update(
                """
                insert into social_accounts(user_id, provider, provider_user_id, email, created_at)
                values (?, 'APPLE', ?, null, current_timestamp)
                """,
                userId,
                providerUserId
        );
        jdbcTemplate.update(
                """
                insert into saved_places(
                    user_id, alias, type, display_name, address,
                    latitude, longitude, created_at, updated_at
                )
                values (?, '회사', 'PLACE', '회사', '서울', 37.5, 127.0, current_timestamp, current_timestamp)
                """,
                userId
        );
        long memberSearchId = insertDepartureSearch(userId, null, "withdraw-member-search");
        insertDepartureSearchCandidate(memberSearchId);
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

    private long insertDepartureSearch(Long userId, Long meetingId, String keyword) {
        jdbcTemplate.update(
                """
                insert into departure_place_searches(
                    user_id, meeting_id, keyword, provider, execution_path, created_at
                )
                values (?, ?, ?, 'KAKAO_LOCAL', 'KEYWORD', current_timestamp)
                """,
                userId,
                meetingId,
                keyword
        );
        return jdbcTemplate.queryForObject(
                "select id from departure_place_searches where keyword = ?",
                Long.class,
                keyword
        );
    }

    private void insertDepartureSearchCandidate(long searchId) {
        jdbcTemplate.update(
                """
                insert into departure_place_search_candidates(
                    search_id, position, type, display_name, address, latitude, longitude
                )
                values (?, 1, 'PLACE', '검색 후보', '서울', 37.5, 127.0)
                """,
                searchId
        );
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
}
