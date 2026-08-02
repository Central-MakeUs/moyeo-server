package com.moyeo.controller.meeting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.controller.TestMemberFactory;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.commercial.CommercialAreaRepository;
import com.moyeo.service.meeting.MeetingService;
import com.moyeo.service.meeting.MeetingCoverStorage;
import com.moyeo.service.meeting.SaveParticipationCommand;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaType;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.route.KakaoRouteClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingParticipantRepository meetingParticipantRepository;

    @Autowired
    private MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository;

    @Autowired
    private CommercialAreaRepository commercialAreaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private TestMemberFactory testMemberFactory;

    @MockitoBean
    private MeetingCoverStorage meetingCoverStorage;

    @MockitoBean
    private KakaoLoginService kakaoLoginService;

    @MockitoBean
    private KakaoRouteClient kakaoRouteClient;

    @Test
    void localProfileLoadsConfirmedCommercialAreaCatalog() {
        var areas = commercialAreaRepository.findAllBySourceAndAreaTypeInOrderByExternalCodeAsc(
                CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS,
                List.of(CommercialAreaType.DEVELOPMENT, CommercialAreaType.TOURIST_SPECIAL)
        );

        assertThat(areas).hasSize(255);
        assertThat(areas.stream().filter(area -> area.getAreaType() == CommercialAreaType.DEVELOPMENT)).hasSize(249);
        assertThat(areas.stream().filter(area -> area.getAreaType() == CommercialAreaType.TOURIST_SPECIAL)).hasSize(6);
    }

    @Test
    void homeListsHostNicknameAndMeetingDetailMarksCurrentParticipant() throws Exception {
        String hostToken = signupAndGetAccessToken("home-detail-host", "homehost");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        Long meetingId = jdbcTemplate.queryForObject(
                "select id from meetings where invite_code = ?",
                Long.class,
                inviteCode
        );
        String memberToken = signupAndGetAccessToken("home-detail-member", "homemember");
        joinMember(inviteCode, memberToken, "member-in-meeting");

        mockMvc.perform(get("/api/meetings/me")
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planningMeetings[0].inviteCode").value(inviteCode))
                .andExpect(jsonPath("$.planningMeetings[0].hostNickname").value("homehost"))
                .andExpect(jsonPath("$.planningMeetings[0].role").value("HOST"));

        mockMvc.perform(get("/api/meetings/{meetingId}", meetingId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").value(meetingId))
                .andExpect(jsonPath("$.hostNickname").value("homehost"))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.participants[0].isMe").value(false))
                .andExpect(jsonPath("$.participants[1].isMe").value(true));

        String outsiderToken = signupAndGetAccessToken("home-detail-outsider", "outsider");
        mockMvc.perform(get("/api/meetings/{meetingId}", meetingId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEETING_NOT_FOUND"));
    }

    @Test
    void createMeetingReturnsMeetingAndInvitationInformation() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost1", "host1");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.inviteCode").isString())
                .andExpect(jsonPath("$.invitePath").doesNotExist());
    }

    @Test
    void createDateOnlyMeetingDoesNotRequireCommonTimeRange() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-date-only", "host-date-only");

        Long meetingId = createMeetingAndGetMeetingId(accessToken, dateOnlyCreateMeetingRequest());

        assertThat(jdbcTemplate.queryForObject(
                "select schedule_input_type from meetings where id = ?",
                String.class,
                meetingId
        )).isEqualTo("DATE_ONLY");
        assertThat(jdbcTemplate.queryForObject(
                "select available_start_time is null and available_end_time is null from meetings where id = ?",
                Boolean.class,
                meetingId
        )).isTrue();
    }

    @Test
    void createMeetingWithoutDeadlineStoresNullDeadlineAndAllowsGuestJoin() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-no-deadline", "host-no-deadline");
        ObjectNode request = objectMapper.valueToTree(defaultCreateMeetingRequest(6));
        request.remove("deadlineMinutes");
        request.put("noDeadline", true);

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String inviteCode = objectMapper.readTree(response).get("inviteCode").asText();

        assertThat(jdbcTemplate.queryForObject(
                "select deadline_at is null from meetings where invite_code = ?",
                Boolean.class,
                inviteCode
        )).isTrue();

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineAt").doesNotExist())
                .andExpect(jsonPath("$.participationStatus.canJoin").value(true));

        joinGuest(inviteCode, "guest-no-deadline");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlineAt").doesNotExist())
                .andExpect(jsonPath("$.remainingMinutes").doesNotExist());
    }

    @Test
    void createMeetingAllowsDeadlineUpToSevenDays() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-seven-day-deadline", "host-seven-day-deadline");
        ObjectNode sevenDayDeadline = objectMapper.valueToTree(defaultCreateMeetingRequest(6));
        sevenDayDeadline.put("deadlineMinutes", 10_080);

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sevenDayDeadline)))
                .andExpect(status().isCreated());
    }

    @Test
    void createMeetingRejectsDeadlineBeyondSevenDays() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-over-seven-day-deadline", "host-over-seven-day-deadline");
        ObjectNode overSevenDayDeadline = objectMapper.valueToTree(defaultCreateMeetingRequest(6));
        overSevenDayDeadline.put("deadlineMinutes", 10_090);

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overSevenDayDeadline)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingValidatesDeadlineFieldsAccordingToNoDeadline() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-no-deadline-validation", "host-no-deadline-validation");

        ObjectNode deadlineWithoutValue = objectMapper.valueToTree(defaultCreateMeetingRequest(6));
        deadlineWithoutValue.remove("deadlineMinutes");
        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deadlineWithoutValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));

        ObjectNode noDeadlineWithMinutes = objectMapper.valueToTree(defaultCreateMeetingRequest(6));
        noDeadlineWithMinutes.put("noDeadline", true);
        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noDeadlineWithMinutes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsScheduleInputTypeAndTimeRangeMismatch() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-input-mismatch", "host-input-mismatch");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "invalid-date-only",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_ONLY",
                                "availableStartTime", "09:00",
                                "availableEndTime", "18:00",
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "invalid-place-only",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "scheduleInputType", "DATE_ONLY",
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createPlaceOnlyMeetingWithoutScheduleInputType() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-place-simple", "host-place-simple");

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "place-simple",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "서울 강남구",
                                        "transportationMode", "PUBLIC_TRANSIT"
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long meetingId = objectMapper.readTree(response).get("meetingId").asLong();
        assertThat(jdbcTemplate.queryForObject(
                "select schedule_input_type from meetings where id = ?",
                String.class,
                meetingId
        )).isEqualTo("NONE");
        assertThat(jdbcTemplate.queryForObject(
                "select place_recommendation_strategy from meetings where id = ?",
                String.class,
                meetingId
        )).isEqualTo("MIDDLE_POINT");
    }

    @Test
    void createMeetingWithOptionalCoverReturnsVersionedCoverUrl() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-cover", "host-cover");
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(defaultCreateMeetingRequest(6))
        );
        MockMultipartFile coverImage = new MockMultipartFile(
                "coverImage",
                "cover.png",
                MediaType.IMAGE_PNG_VALUE,
                pngImage()
        );

        mockMvc.perform(multipart("/api/meetings")
                        .file(request)
                        .file(coverImage)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist());

        verify(meetingCoverStorage).put(any(String.class), any(byte[].class));
    }

    @Test
    void createMeetingWithMultipartRequestAndNoCoverSucceeds() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-no-cover", "host-no-cover");
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(defaultCreateMeetingRequest(6))
        );

        mockMvc.perform(multipart("/api/meetings")
                .file(request)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.inviteCode").isString())
                .andExpect(jsonPath("$.coverImageUrl").doesNotExist());
    }

    @Test
    void hostCanReplaceAndDeleteCoverImage() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-cover-edit", "host-cover-edit");
        String created = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long meetingId = objectMapper.readTree(created).get("meetingId").asLong();
        MockMultipartFile coverImage = new MockMultipartFile(
                "coverImage", "cover.png", MediaType.IMAGE_PNG_VALUE, pngImage());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/meetings/{meetingId}/cover-image", meetingId)
                        .file(coverImage)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverImageUrl").value(org.hamcrest.Matchers.containsString("/cover-image?v=")));

        mockMvc.perform(delete("/api/meetings/{meetingId}/cover-image", meetingId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        verify(meetingCoverStorage).delete(anyString());
    }

    @Test
    void createScheduleMeetingStoresHostParticipationAtomically() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost35", "host35");

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(6))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long meetingId = objectMapper.readTree(response).get("meetingId").asLong();
        Long hostParticipantId = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId).getFirst().getId();
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(hostParticipantId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_schedule_candidates where meeting_id = ?",
                Long.class,
                meetingId
        )).isEqualTo(2L);
    }

    @Test
    void createDateOnlyMeetingUsesCandidateDatesAsHostAvailability() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-date-only-complete", "host-date-only-complete");
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dateOnlyCreateMeetingRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long meetingId = objectMapper.readTree(response).get("meetingId").asLong();
        Long hostParticipantId = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId).getFirst().getId();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participant_schedule_date_availabilities where participant_id = ?",
                Long.class,
                hostParticipantId
        )).isEqualTo(2L);
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(hostParticipantId)).isZero();

        String inviteCode = objectMapper.readTree(response).get("inviteCode").asText();
        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.availableStartTime").doesNotExist())
                .andExpect(jsonPath("$.availableEndTime").doesNotExist());
    }

    @Test
    void createMeetingRollsBackWhenHostAvailabilityIsInvalid() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-rollback", "host-rollback");
        Long meetingCount = jdbcTemplate.queryForObject("select count(*) from meetings", Long.class);

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "rollback",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_AND_TIME",
                                "availableStartTime", "09:00",
                                "availableEndTime", "18:00",
                                "scheduleCandidateDates", List.of("2026-07-01"),
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability("08:00", "09:00"))
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from meetings", Long.class)).isEqualTo(meetingCount);
    }

    @Test
    void createMeetingRequiresAccessToken() throws Exception {
        mockMvc.perform(post("/api/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend-meeting",
                                "maxParticipants", 6
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void createMeetingValidatesRequest() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost2", "host2");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCreateMeetingRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsMaxParticipantsBelowMinimum() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-min-participants", "host-min-participants");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(1))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsMaxParticipantsAboveMaximum() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-max-participants", "host-max-participants");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateMeetingRequest(21))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsMissingMaxParticipants() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-missing-participants", "host-missing-participants");
        ObjectNode request = objectMapper.valueToTree(defaultCreateMeetingRequest(6));
        request.remove("maxParticipants");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("maxParticipants"));
    }

    @Test
    void createMeetingRejectsNullMaxParticipants() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-null-participants", "host-null-participants");
        ObjectNode request = objectMapper.valueToTree(defaultCreateMeetingRequest(6));
        request.putNull("maxParticipants");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("maxParticipants"));
    }

    @Test
    void createMeetingAcceptsTwentyOneScheduleCandidateDates() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-twenty-one-dates", "host-twenty-one-dates");
        List<LocalDate> candidateDates = java.util.stream.IntStream.range(0, 21)
                .mapToObj(dayOffset -> LocalDate.of(2026, 7, 1).plusDays(dayOffset))
                .toList();

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "three weeks",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_AND_TIME",
                                "availableStartTime", "18:00",
                                "availableEndTime", "22:00",
                                "scheduleCandidateDates", candidateDates,
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability("18:00", "19:00"))
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated());
    }

    @Test
    void createMeetingRejectsMoreThanTwentyOneScheduleCandidateDates() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-many-dates", "host-many-dates");
        List<LocalDate> candidateDates = java.util.stream.IntStream.range(0, 22)
                .mapToObj(dayOffset -> LocalDate.of(2026, 7, 1).plusDays(dayOffset))
                .toList();

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_AND_TIME",
                                "availableStartTime", "18:00",
                                "availableEndTime", "22:00",
                                "scheduleCandidateDates", candidateDates,
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability("18:00", "19:00"))
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("scheduleCandidateDates"));
    }

    @Test
    void createMeetingRejectsNonHourUnitScheduleTime() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost10", "host10");

        CreateMeetingRequest request = createMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(18, 30),
                LocalTime.of(22, 0),
                1440
        );

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsNonTenMinuteDeadline() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost11", "host11");

        CreateMeetingRequest request = createMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                15
        );

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createMeetingRejectsMiddlePointWithoutDeparture() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost17", "host17");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    void createMeetingSupportsPlaceOnlyPlanning() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost13", "host13");

        CreateMeetingRequest request = createMeetingRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                1440
        );

        String inviteCode = createMeetingAndGetInviteCode(accessToken, request);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planningType").value("PLACE_ONLY"))
                .andExpect(jsonPath("$.scheduleMode").value("NONE"))
                .andExpect(jsonPath("$.scheduleCandidateDates").isEmpty())
                .andExpect(jsonPath("$.availableStartTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.availableEndTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"));
    }

    @Test
    void placeViewUsesDepartureAddressWhenDepartureNameIsOmitted() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-departure-address", "host-departure-address");
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "주소 출발지",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "departure", Map.of(
                                        "address", "서울 강남구 테헤란로 123",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String inviteCode = objectMapper.readTree(response).get("inviteCode").asText();

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationBasis").value("STRAIGHT_LINE_PREVIEW"))
                .andExpect(jsonPath("$.participants[0].departureName").value("서울 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.participants[0].departureAddress").value("서울 강남구 테헤란로 123"))
                .andExpect(jsonPath("$.recommendations[0].areaName").isString());
    }

    @Test
    void createMeetingAcceptsGyeonggiDepartureAndRejectsOtherRegions() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost-departure-region", "host-departure-region");

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "gyeonggi",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "departure", Map.of(
                                        "address", "경기도 성남시 분당구",
                                        "latitude", 37.359571,
                                        "longitude", 127.105399,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "outside",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "departure", Map.of(
                                        "address", "부산광역시 해운대구",
                                        "latitude", 35.163134,
                                        "longitude", 129.163547,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    void createMeetingRemovesDuplicatedScheduleCandidateDatesAndSortsThem() throws Exception {
        String accessToken = signupAndGetAccessToken("meetinghost14", "host14");

        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "weekend",
                                "maxParticipants", 6,
                                "planningType", "SCHEDULE_ONLY",
                                "scheduleInputType", "DATE_AND_TIME",
                                "availableStartTime", "18:00",
                                "availableEndTime", "22:00",
                                "scheduleCandidateDates", List.of("2026-07-02", "2026-07-01", "2026-07-01"),
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability("18:00", "19:00"))
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String inviteCode = objectMapper.readTree(response).get("inviteCode").asText();

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleCandidateDates.length()").value(2))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.scheduleCandidateDates[1]").value("2026-07-02"));
    }

    @Test
    void getInvitationReturnsMeetingInfo() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost3", "host3", 6);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.name").value("weekend-meeting"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.planningType").value("SCHEDULE_AND_PLACE"))
                .andExpect(jsonPath("$.scheduleMode").value("VOTE"))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.deadlineAt").isString())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.hostNickname").value("host3"))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(true))
                .andExpect(jsonPath("$.participationStatus.reason").value("AVAILABLE"))
                .andExpect(jsonPath("$.participationStatus.message").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getInvitationReturnsAlreadyJoinedStatusForAuthenticatedParticipant() throws Exception {
        String hostToken = signupAndGetAccessToken("invitation-already-joined-host", "already-joined-host");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("ALREADY_JOINED"))
                .andExpect(jsonPath("$.participationStatus.message").value("이미 참여 중인 모임이에요."));
    }

    @Test
    void getInvitationPrioritizesAlreadyJoinedStatusOverDeadlineAndParticipantLimit() throws Exception {
        String hostToken = signupAndGetAccessToken("invitation-already-joined-priority-host", "already-joined-priority-host");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(2));
        joinGuest(inviteCode, "already-joined-priority-guest");
        jdbcTemplate.update("update meetings set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?", inviteCode);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("ALREADY_JOINED"));
    }

    @Test
    void getInvitationKeepsAvailableStatusForAuthenticatedNonParticipant() throws Exception {
        String hostToken = signupAndGetAccessToken("invitation-non-participant-host", "non-participant-host");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        String otherToken = signupAndGetAccessToken("invitation-non-participant", "non-participant");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationStatus.canJoin").value(true))
                .andExpect(jsonPath("$.participationStatus.reason").value("AVAILABLE"));
    }

    @Test
    void getInvitationAllowsPendingOnboardingUserWithAccessToken() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("invitation-pending-user-host", "pending-user-host", 6);
        String pendingToken = testMemberFactory.createPendingAccessToken();

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode)
                        .header("Authorization", "Bearer " + pendingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participationStatus.canJoin").value(true))
                .andExpect(jsonPath("$.participationStatus.reason").value("AVAILABLE"));
    }

    @Test
    void getInvitationRejectsInvalidAccessToken() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("invitation-invalid-token-host", "invalid-token-host", 6);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode)
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void getInvitationRejectsUnknownInviteCode() throws Exception {
        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", "UNKNOWN123"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEETING_INVITATION_NOT_FOUND"));
    }

    @Test
    void getInvitationReturnsParticipantLimitExceededStatus() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost15", "host15", 2);
        joinGuest(inviteCode, "guest-limit");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("PARTICIPANT_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.participationStatus.message").value("\uBAA8\uC778 \uC778\uC6D0\uC774 \uBAA8\uB450 \uCC3C\uC5B4\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694."));
    }

    @Test
    void getInvitationReturnsDeadlinePassedStatusBeforeParticipantLimitStatus() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost16", "host16", 2);
        joinGuest(inviteCode, "guest-deadline-status");
        jdbcTemplate.update("update meetings set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?", inviteCode);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("DEADLINE_PASSED"))
                .andExpect(jsonPath("$.participationStatus.message").value("\uAE30\uD55C\uC774 \uC9C0\uB09C \uBAA8\uC784\uC774\uC5D0\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694."));
    }

    @Test
    void joinGuestCreatesGuestParticipant() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost4", "host4", 6);

        String response = mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("guest", "1234"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.participantId").isNumber())
                .andExpect(jsonPath("$.nickname").value("guest"))
                .andExpect(jsonPath("$.participantType").value("GUEST"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long participantId = objectMapper.readTree(response).get("participantId").asLong();
        var participant = meetingParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getPasswordHash()).isNotEqualTo("1234");
        assertThat(passwordEncoder.matches("1234", participant.getPasswordHash())).isTrue();
        assertThat(participant.getDepartureName()).isEqualTo("company");
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(1);
    }

    @Test
    void joinGuestRejectsInvalidParticipationAndRollsBackParticipantCreation() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost-invalid", "host-invalid", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "invalid",
                                "password", "1234",
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "08:00",
                                                "endTime", "09:00"
                                        ))
                                ),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "서울 강남구",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participants where meeting_id = (select id from meetings where invite_code = ?)",
                Long.class,
                inviteCode
        )).isEqualTo(1L);
    }

    @Test
    void joinGuestRejectsDuplicatedNicknameInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost5", "host5", 6);
        joinGuest(inviteCode, "duplicate");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("duplicate", "1234"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEETING_PARTICIPANT_NICKNAME"));
    }

    @Test
    void joinGuestAllowsMultipleGuestsWithNullUserId() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost9", "host9", 6);

        joinGuest(inviteCode, "guest-null-user-1");
        joinGuest(inviteCode, "guest-null-user-2");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    void joinGuestAllowsHostNicknameInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost6", "hostname", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("hostname", "1234"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("hostname"))
                .andExpect(jsonPath("$.participantType").value("GUEST"));
    }

    @Test
    void joinGuestRejectsExceededParticipantLimit() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost7", "host7", 2);
        joinGuest(inviteCode, "guest-limit-existing");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("guestlimit", "1234"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPANT_LIMIT_EXCEEDED"));
    }

    @Test
    void joinGuestRejectsClosedMeetingParticipation() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost12", "host12", 6);
        jdbcTemplate.update("update meetings set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?", inviteCode);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest("guestclose", "1234"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPATION_CLOSED"));
    }

    @Test
    void joinGuestValidatesRequest() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost8", "host8", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void joinGuestValidatesNicknameAndFourDigitPassword() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost-guest-validation", "hostguestvalidation", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "Test1", "password", "1234"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "guest", "password", "abcd"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void hostAndMemberCanReadAndIndependentlyModifyTheirParticipationResponses() throws Exception {
        String hostToken = signupAndGetAccessToken("participation-update-host", "update-host");
        String memberToken = signupAndGetAccessToken("participation-update-member", "update-member");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        joinMember(inviteCode, memberToken, "update-member-room");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/members/me/participation", inviteCode)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantType").value("HOST"))
                .andExpect(jsonPath("$.scheduleResponse.availableTimeRanges[0].candidateDate").value("2026-07-01"))
                .andExpect(jsonPath("$.departure.name").value("company"));

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/schedule-response", inviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availableTimeRanges": [
                                    { "candidateDate": "2026-07-02", "startTime": "10:00", "endTime": "12:00" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleResponse.availableTimeRanges[0].candidateDate").value("2026-07-02"))
                .andExpect(jsonPath("$.departure.name").value("company"));

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/departure", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "member-home",
                                  "address": "서울 마포구 월드컵북로 120",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780,
                                  "transportationMode": "CAR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantType").value("MEMBER"))
                .andExpect(jsonPath("$.scheduleResponse.availableTimeRanges[0].candidateDate").value("2026-07-01"))
                .andExpect(jsonPath("$.departure.name").value("member-home"))
                .andExpect(jsonPath("$.departure.transportationMode").value("CAR"));

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/members/me/participation", inviteCode)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleResponse.availableTimeRanges[0].candidateDate").value("2026-07-02"))
                .andExpect(jsonPath("$.departure.name").value("company"));
    }

    @Test
    void participationResponseUpdatesRejectUnsupportedMeetingInputs() throws Exception {
        String hostToken = signupAndGetAccessToken("participation-update-invalid", "update-invalid");
        String placeOnlyResponse = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "place-update",
                                "maxParticipants", 6,
                                "planningType", "PLACE_ONLY",
                                "departure", Map.of(
                                        "address", "서울 강남구 테헤란로 123",
                                        "transportationMode", "PUBLIC_TRANSIT"
                                ),
                                "deadlineMinutes", 1440
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String placeOnlyInviteCode = objectMapper.readTree(placeOnlyResponse).path("inviteCode").asText();
        String scheduleOnlyInviteCode = createMeetingAndGetInviteCode(hostToken, dateOnlyCreateMeetingRequest());

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/schedule-response", placeOnlyInviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availableDates\":[\"2026-07-01\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/departure", scheduleOnlyInviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "서울 마포구 월드컵북로 120",
                                  "transportationMode": "CAR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    void hostCanReplaceDateOnlyResponseWithSubsetOfExistingCandidateDates() throws Exception {
        String hostToken = signupAndGetAccessToken("host-date-response-update", "host-date-update");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, dateOnlyCreateMeetingRequest());

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/schedule-response", inviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"availableDates\":[\"2026-07-02\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantType").value("HOST"))
                .andExpect(jsonPath("$.scheduleResponse.availableDates[0]").value("2026-07-02"));

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availabilityStatuses.length()").value(1))
                .andExpect(jsonPath("$.availabilityStatuses[0].candidateDate").value("2026-07-02"))
                .andExpect(jsonPath("$.availabilityStatuses[0].availableParticipantCount").value(1));
    }

    @Test
    void participationResponseUpdatesRejectNonParticipantsAndConfirmedMeetings() throws Exception {
        String hostToken = signupAndGetAccessToken("participation-closed-host", "closed-host");
        String memberToken = signupAndGetAccessToken("participation-closed-member", "closed-member");
        String outsiderToken = signupAndGetAccessToken("participation-closed-outsider", "closed-outsider");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/schedule-response", inviteCode)
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availableTimeRanges": [
                                    { "candidateDate": "2026-07-01", "startTime": "10:00", "endTime": "12:00" }
                                  ]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPANT_NOT_FOUND"));

        joinMember(inviteCode, memberToken, "closed-member-room");
        confirmMeeting(getMeetingId(inviteCode), inviteCode, hostToken);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/members/me/participation", inviteCode)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantType").value("HOST"));

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/schedule-response", inviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availableTimeRanges": [
                                    { "candidateDate": "2026-07-02", "startTime": "10:00", "endTime": "12:00" }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPATION_CLOSED"));
    }

    @Test
    void participationResponseGetAllowsDeadlinePassedMeeting() throws Exception {
        String hostToken = signupAndGetAccessToken("participation-deadline-read-host", "deadline-read-host");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        jdbcTemplate.update(
                "update meetings set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?",
                inviteCode
        );

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/members/me/participation", inviteCode)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantType").value("HOST"));
    }

    @Test
    void participationResponseUpdatesRejectDeadlinePassedMeeting() throws Exception {
        String hostToken = signupAndGetAccessToken("participation-deadline-update-host", "deadline-update-host");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        jdbcTemplate.update(
                "update meetings set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?",
                inviteCode
        );

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/schedule-response", inviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "availableTimeRanges": [
                                    { "candidateDate": "2026-07-02", "startTime": "10:00", "endTime": "12:00" }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPATION_CLOSED"));

        mockMvc.perform(patch("/api/meetings/invitations/{inviteCode}/members/me/participation/departure", inviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "address": "서울 마포구 월드컵북로 120",
                                  "transportationMode": "CAR"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPATION_CLOSED"));
    }

    @Test
    void joinMemberCreatesMemberParticipant() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost22", "host22", 6);
        String memberToken = signupAndGetAccessToken("memberjoin1", "default-member");

        String response = mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("meeting-member"))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.participantId").isNumber())
                .andExpect(jsonPath("$.nickname").value("meeting-member"))
                .andExpect(jsonPath("$.participantType").value("MEMBER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long participantId = objectMapper.readTree(response).get("participantId").asLong();
        var participant = meetingParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getPasswordHash()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participants where id = ? and user_id is not null and participant_type = 'MEMBER'",
                Long.class,
                participantId
        )).isEqualTo(1L);
    }

    @Test
    void joinMemberRequiresAccessToken() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost23", "host23", 6);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("meeting-member"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void joinMemberRejectsSameMemberInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost24", "host24", 6);
        String memberToken = signupAndGetAccessToken("memberjoin2", "member2");
        joinMember(inviteCode, memberToken, "meeting-member1");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("meeting-member2"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEETING_PARTICIPANT_MEMBER"));
    }

    @Test
    void joinMemberAllowsGuestNicknameInSameMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost25", "host25", 6);
        String memberToken = signupAndGetAccessToken("memberjoin3", "member3");
        joinGuest(inviteCode, "duplicated-name");

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("duplicated-name"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("duplicated-name"))
                .andExpect(jsonPath("$.participantType").value("MEMBER"));
    }

    @Test
    void joinMemberRejectsHostUserInSameMeeting() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost26", "host26");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest("host-as-member"))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEETING_PARTICIPANT_MEMBER"));
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationStoresScheduleAvailabilitiesAndDeparture() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost18", "host18", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-participation");

        mockMvc.perform(put("/api/meetings/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(
                                                Map.of(
                                                        "candidateDate", "2026-07-01",
                                                        "startTime", "09:00",
                                                        "endTime", "10:00"
                                                ),
                                                Map.of(
                                                        "candidateDate", "2026-07-01",
                                                        "startTime", "10:00",
                                                        "endTime", "11:00"
                                                )
                                        )
                                ),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "서울 강남구",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantId").value(participantId))
                .andExpect(jsonPath("$.scheduleAvailabilityCount").value(2))
                .andExpect(jsonPath("$.hasDeparture").value(true));

        var participant = meetingParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getDepartureName()).isEqualTo("company");
        assertThat(participant.getDepartureAddress()).isEqualTo("서울 강남구");
        assertThat(participant.getTransportationMode()).isEqualTo(com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT);
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(2);
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationReplacesPreviousScheduleAvailabilities() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost19", "host19", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-replace");

        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");
        saveDefaultParticipation(inviteCode, participantId, "11:00", "12:00");

        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(1);
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationRejectsOutOfRangeScheduleAvailability() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost20", "host20", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-invalid-time");

        mockMvc.perform(put("/api/meetings/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "08:00",
                                                "endTime", "09:00"
                                        ))
                                ),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "서울 강남구",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    @Disabled("참여 생성과 상세 정보 저장은 POST 참여 API로 통합됨")
    void saveParticipationRejectsDepartureForScheduleOnlyMeeting() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode(
                "meetinghost21",
                "host21",
                createMeetingRequest(
                        "schedule",
                        "schedule only",
                        6,
                        com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                        ScheduleInputType.DATE_AND_TIME,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        1440
                )
        );
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-only");

        mockMvc.perform(put("/api/meetings/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(Map.of(
                                                "candidateDate", "2026-07-01",
                                                "startTime", "09:00",
                                                "endTime", "10:00"
                                        ))
                                ),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "서울 강남구",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));
    }

    @Test
    void getMeetingViewReturnsParticipantListWithoutRedundantResponseStatus() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost29", "host29", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-view");
        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("weekend-meeting"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.responseRate").doesNotExist())
                .andExpect(jsonPath("$.participants[0].participantType").value("HOST"))
                .andExpect(jsonPath("$.participants[0].withdrawn").value(false))
                .andExpect(jsonPath("$.participants[1].nickname").value("guestview"))
                .andExpect(jsonPath("$.participants[1].withdrawn").value(false))
                .andExpect(jsonPath("$.participants[0].scheduleResponded").doesNotExist())
                .andExpect(jsonPath("$.participants[0].placeResponded").doesNotExist())
                .andExpect(jsonPath("$.participants[0].responseCompleted").doesNotExist());
    }

    @Test
    void hostCanDeleteConfirmedMeetingAndAllMeetingData() throws Exception {
        String hostToken = signupAndGetAccessToken("meeting-delete-host", "host-delete");
        String otherToken = signupAndGetAccessToken("meeting-delete-other", "other-delete");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        Long meetingId = getMeetingId(inviteCode);
        joinGuest(inviteCode, "delete-guest");
        confirmMeeting(meetingId, inviteCode, hostToken);
        List<Long> participantIds = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId).stream()
                .map(participant -> participant.getId())
                .toList();
        jdbcTemplate.update("update meetings set cover_image_key = ? where id = ?", "meeting-covers/delete-test.jpg", meetingId);
        jdbcTemplate.update(
                """
                insert into departure_place_searches(meeting_id, keyword, provider, execution_path, created_at)
                values (?, 'delete-search', 'KAKAO_LOCAL', 'KEYWORD', current_timestamp)
                """,
                meetingId
        );

        mockMvc.perform(delete("/api/meetings/{meetingId}", meetingId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEETING_DELETION_FORBIDDEN"));

        mockMvc.perform(delete("/api/meetings/{meetingId}", meetingId)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEETING_INVITATION_NOT_FOUND"));

        assertThat(countRows("meeting_participants", "meeting_id", meetingId)).isZero();
        assertThat(countRows("meeting_schedule_candidates", "meeting_id", meetingId)).isZero();
        assertThat(countRows("departure_place_searches", "meeting_id", meetingId)).isZero();
        assertThat(participantIds).allSatisfy(participantId ->
                assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isZero()
        );
        verify(meetingCoverStorage).delete("meeting-covers/delete-test.jpg");
    }

    @Test
    void memberCanLeaveMeetingButHostCannot() throws Exception {
        String hostToken = signupAndGetAccessToken("meeting-leave-host", "host-leave");
        String memberToken = signupAndGetAccessToken("meeting-leave-member", "member-leave");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        Long meetingId = getMeetingId(inviteCode);
        joinMember(inviteCode, memberToken, "member-room");
        confirmMeeting(meetingId, inviteCode, hostToken);
        Long memberParticipantId = meetingParticipantRepository.findByMeetingIdAndUserId(
                meetingId,
                jwtTokenProvider.parse(memberToken).userId()
        ).orElseThrow().getId();

        mockMvc.perform(delete("/api/meetings/{meetingId}/participation", meetingId)
                        .header("Authorization", "Bearer " + hostToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEETING_PARTICIPANT_LEAVE_FORBIDDEN"));

        mockMvc.perform(delete("/api/meetings/{meetingId}/participation", meetingId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participants[0].participantType").value("HOST"));

        mockMvc.perform(get("/api/meetings/{meetingId}", meetingId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEETING_NOT_FOUND"));

        assertThat(countRows("meeting_participants", "meeting_id", meetingId)).isEqualTo(1L);
        assertThat(meetingParticipantScheduleAvailabilityRepository.countByParticipantId(memberParticipantId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from meeting_participant_schedule_availabilities availability
                join meeting_participants participant on participant.id = availability.participant_id
                where participant.meeting_id = ?
                """,
                Long.class,
                meetingId
        )).isEqualTo(1L);
    }

    @Test
    void memberLeavingConfirmedDateOnlyMeetingDeletesDateAvailability() throws Exception {
        String hostToken = signupAndGetAccessToken("date-only-leave-host", "date-host");
        String memberToken = signupAndGetAccessToken("date-only-leave-member", "date-member");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, dateOnlyCreateMeetingRequest());
        Long meetingId = getMeetingId(inviteCode);
        joinDateOnlyMember(inviteCode, memberToken, "date-member-room");
        confirmDateOnlyMeeting(meetingId, hostToken);
        Long memberParticipantId = meetingParticipantRepository.findByMeetingIdAndUserId(
                meetingId,
                jwtTokenProvider.parse(memberToken).userId()
        ).orElseThrow().getId();

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participant_schedule_date_availabilities where participant_id = ?",
                Long.class,
                memberParticipantId
        )).isEqualTo(1L);

        mockMvc.perform(delete("/api/meetings/{meetingId}/participation", meetingId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participant_schedule_date_availabilities where participant_id = ?",
                Long.class,
                memberParticipantId
        )).isZero();
    }

    @Test
    void hostAndMemberCanChangeOnlyTheirOwnMeetingNicknameWithDuplicatesAllowed() throws Exception {
        String hostToken = signupAndGetAccessToken("meeting-nickname-host", "host-default");
        String memberToken = signupAndGetAccessToken("meeting-nickname-member", "member-default");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));
        Long meetingId = getMeetingId(inviteCode);
        joinMember(inviteCode, memberToken, "member-room");

        mockMvc.perform(patch("/api/meetings/{meetingId}/participants/me/nickname", meetingId)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"same\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("same"));

        mockMvc.perform(patch("/api/meetings/{meetingId}/participants/me/nickname", meetingId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"same\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("same"));

        mockMvc.perform(patch("/api/meetings/{meetingId}/participants/me/nickname", meetingId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"same1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants[0].nickname").value("same"))
                .andExpect(jsonPath("$.participants[1].nickname").value("same"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("member-default"));
    }

    @Test
    void withdrawnMemberIsDeletedFromMeetingAndPlaceViews() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("withdraw-view-host", "withdraw-view-host", 6);
        String memberToken = signupAndGetAccessToken("withdraw-view-member", "withdraw-view-member");
        Long memberUserId = jwtTokenProvider.parse(memberToken).userId();
        String providerUserId = "withdraw-view-provider";
        jdbcTemplate.update(
                """
                insert into social_accounts(user_id, provider, provider_user_id, email, created_at)
                values (?, 'KAKAO', ?, null, current_timestamp)
                """,
                memberUserId,
                providerUserId
        );
        joinMember(inviteCode, memberToken, "withdrawn-snapshot");

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());

        verify(kakaoLoginService).disconnectStoredAccount(providerUserId);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].withdrawn").value(false));

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].withdrawn").value(false));
    }

    @Test
    void getScheduleViewReturnsTopAvailabilitySlots() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost30", "host30", 6);
        Long firstParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-view-1");
        Long secondParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-view-2");
        saveDefaultParticipation(inviteCode, firstParticipantId, "09:00", "10:00");
        saveDefaultParticipation(inviteCode, secondParticipantId, "09:00", "10:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sort").value("LONGEST_MEETING"))
                .andExpect(jsonPath("$.participantCount").value(3))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].candidateDate").value("2026-07-01"))
                .andExpect(jsonPath("$.candidates[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.candidates[0].availableParticipantCount").value(3))
                .andExpect(jsonPath("$.candidates[0].availableParticipants.length()").value(3))
                .andExpect(jsonPath("$.candidates[0].availableParticipants[0].nickname").value("host30"))
                .andExpect(jsonPath("$.availabilityStatuses[0].availableParticipantCount").value(3))
                .andExpect(jsonPath("$.candidates[0].totalParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.emptyMessage").doesNotExist());
    }

    @Test
    void swaggerDocumentsAllScheduleViewInputTypes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.ScheduleViewResponse.properties.scheduleInputType.enum",
                        containsInAnyOrder("DATE_ONLY", "DATE_AND_TIME", "NONE")
                ));
    }

    @Test
    void getScheduleViewUsesEarliestDateByDefaultAndOmitsCandidatesWithoutOverlap() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost-schedule-no-overlap", "host-no-overlap", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-no-overlap");
        saveDefaultParticipation(inviteCode, participantId, "12:00", "13:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort").value("EARLIEST_DATE"))
                .andExpect(jsonPath("$.candidates").isEmpty())
                .andExpect(jsonPath("$.availabilityStatuses.length()").value(2))
                .andExpect(jsonPath("$.availabilityStatuses[0].availableParticipantCount").value(1));
    }

    @Test
    void getScheduleViewSortsLongestMeetingCandidatesByDuration() throws Exception {
        String hostToken = signupAndGetAccessToken("schedule-duration-host", "schedule-duration-host");
        CreateMeetingRequest request = new CreateMeetingRequest(
                "sched-duration", "duration ordering", 6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY, ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(9, 0), LocalTime.of(18, 0), List.of(LocalDate.of(2026, 7, 1)),
                new SaveParticipationRequest.ScheduleResponseRequest(
                        null,
                        List.of(new SaveParticipationRequest.ScheduleAvailabilityRequest(
                                LocalDate.of(2026, 7, 1), LocalTime.of(9, 0), LocalTime.of(12, 0)
                        ))
                ),
                null,
                1440
        );
        String inviteCode = createMeetingAndGetInviteCode(hostToken, request);
        joinScheduleOnlyGuest(inviteCode, "durone", "09:00", "10:00");
        joinScheduleOnlyGuest(inviteCode, "durtwo", "10:00", "12:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates.length()").value(2))
                .andExpect(jsonPath("$.candidates[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$.candidates[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$.candidates[1].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.candidates[1].endTime").value("10:00:00"));
    }

    @Test
    void swaggerDocumentsWithdrawnParticipantFields() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.ParticipantResponse.properties.withdrawn.type"
                ).value("boolean"))
                .andExpect(jsonPath(
                        "$.components.schemas.ParticipantDepartureResponse.properties.withdrawn.type"
                ).value("boolean"));
    }

    @Test
    void swaggerDocumentsScheduleInputTypeAsOptionalWithoutNoneForCreation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.properties.scheduleInputType.enum",
                        containsInAnyOrder("DATE_ONLY", "DATE_AND_TIME")
                ))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.required",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("scheduleInputType"))
                ));
    }

    @Test
    void swaggerDocumentsMaxParticipantsAsRequired() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.required",
                        org.hamcrest.Matchers.hasItem("maxParticipants")
                ))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.properties.maxParticipants.minimum"
                ).value(2))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.properties.maxParticipants.maximum"
                ).value(20));
    }

    @Test
    void swaggerDocumentsNoDeadlineRequestAndOmittedDeadlineResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.properties.noDeadline.type"
                ).value("boolean"))
                .andExpect(jsonPath(
                        "$.components.schemas.CreateMeetingRequest.properties.noDeadline.default"
                ).value(false))
                .andExpect(jsonPath(
                        "$.components.schemas.MeetingInvitationResponse.properties.deadlineAt.description"
                ).value(org.hamcrest.Matchers.containsString("마감 없는 모임에서는 반환하지 않습니다")))
                .andExpect(jsonPath(
                        "$.components.schemas.MeetingViewResponse.properties.deadlineAt.description"
                ).value(org.hamcrest.Matchers.containsString("마감 없는 모임에서는 반환하지 않습니다")))
                .andExpect(jsonPath(
                        "$.components.schemas.MeetingViewResponse.properties.remainingMinutes.description"
                ).value(org.hamcrest.Matchers.containsString("마감 없는 모임에서는 반환하지 않습니다")));
    }

    @Test
    void swaggerDocumentsEveryMeetingCreationFlowForJsonAndMultipart() throws Exception {
        List<String> exampleNames = List.of(
                "SCHEDULE_ONLY_DATE_ONLY_NO_DEADLINE",
                "SCHEDULE_AND_PLACE_DATE_AND_TIME",
                "SCHEDULE_AND_PLACE_DATE_ONLY",
                "SCHEDULE_ONLY_DATE_AND_TIME",
                "SCHEDULE_ONLY_DATE_ONLY",
                "PLACE_ONLY"
        );

        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var requestContent = openApi.path("paths")
                .path("/api/meetings")
                .path("post")
                .path("requestBody")
                .path("content");
        assertThat(openApi.path("paths").path("/api/meetings").path("post").path("description").asText())
                .contains("출발지 이름 공통 안내")
                .contains("departure.name");

        var jsonExamples = requestContent.path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var multipartExamples = requestContent.path(MediaType.MULTIPART_FORM_DATA_VALUE).path("examples");
        assertThat(jsonExamples.size()).isEqualTo(exampleNames.size());
        assertThat(multipartExamples.size()).isEqualTo(exampleNames.size());
        assertThat(exampleNames).allSatisfy(exampleName -> {
            assertThat(jsonExamples.has(exampleName)).isTrue();
            assertThat(multipartExamples.has(exampleName)).isTrue();
        });
        assertThat(jsonExamples.path("SCHEDULE_AND_PLACE_DATE_AND_TIME").path("value").has("scheduleCandidateDates")).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_AND_PLACE_DATE_AND_TIME").path("value").has("scheduleResponse")).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_AND_PLACE_DATE_AND_TIME").path("value").has("departure")).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_ONLY_DATE_ONLY").path("value").has("scheduleCandidateDates")).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_ONLY_DATE_ONLY_NO_DEADLINE").path("value").path("noDeadline").asBoolean()).isTrue();
        assertThat(jsonExamples.path("SCHEDULE_ONLY_DATE_ONLY_NO_DEADLINE").path("value").has("deadlineMinutes")).isFalse();
        assertThat(multipartExamples.path("SCHEDULE_ONLY_DATE_ONLY_NO_DEADLINE").path("value").path("request").path("noDeadline").asBoolean()).isTrue();
        assertThat(multipartExamples.path("SCHEDULE_ONLY_DATE_ONLY_NO_DEADLINE").path("value").path("request").has("deadlineMinutes")).isFalse();
        assertThat(jsonExamples.path("PLACE_ONLY").path("value").has("departure")).isTrue();
        assertThat(jsonExamples.path("PLACE_ONLY").path("value").path("departure").has("name")).isFalse();
        assertThat(multipartExamples.path("PLACE_ONLY").path("value").path("request").path("departure").has("name")).isFalse();
        var departureNameSchema = openApi.path("components").path("schemas").path("DepartureRequest")
                .path("properties").path("name");
        assertThat(departureNameSchema.path("type").toString())
                .contains("string")
                .contains("null");
        assertThat(openApi.path("components").path("schemas").path("DepartureRequest").path("required").toString())
                .doesNotContain("name");
    }

    @Test
    void swaggerDocumentsValidationAndParticipationInputErrorsForMeetingFlows() throws Exception {
        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var paths = openApi.path("paths");
        List<String> errorContents = List.of(
                paths.path("/api/meetings").path("post").path("responses").path("400").path("content").toString(),
                paths.path("/api/meetings/invitations/{inviteCode}/guests").path("post").path("responses").path("400").path("content").toString(),
                paths.path("/api/meetings/invitations/{inviteCode}/members").path("post").path("responses").path("400").path("content").toString()
        );

        assertThat(errorContents).allSatisfy(errorContent -> {
            assertThat(errorContent).contains("COMMON_VALIDATION_FAILED");
            assertThat(errorContent).contains("INVALID_MEETING_PARTICIPATION_INPUT");
        });
    }

    @Test
    void swaggerDocumentsSameFiveFlowsForGuestAndMemberJoinAndNoSeparateHostParticipation() throws Exception {
        List<String> exampleNames = List.of(
                "SCHEDULE_AND_PLACE_DATE_AND_TIME",
                "SCHEDULE_AND_PLACE_DATE_ONLY",
                "SCHEDULE_ONLY_DATE_AND_TIME",
                "SCHEDULE_ONLY_DATE_ONLY",
                "PLACE_ONLY"
        );
        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var paths = openApi.path("paths");
        assertThat(paths.path("/api/meetings/{meetingId}/participation").has("post")).isFalse();
        assertThat(paths.path("/api/meetings/{meetingId}/participation").has("delete")).isTrue();
        assertThat(paths.path("/api/meetings/{meetingId}/participants/me/nickname").has("patch")).isTrue();
        var leaveNotFoundExamples = paths.path("/api/meetings/{meetingId}/participation").path("delete")
                .path("responses").path("404").path("content").path("application/problem+json").path("examples");
        var nicknameNotFoundExamples = paths.path("/api/meetings/{meetingId}/participants/me/nickname").path("patch")
                .path("responses").path("404").path("content").path("application/problem+json").path("examples");
        assertThat(leaveNotFoundExamples.has("MEETING_NOT_FOUND")).isTrue();
        assertThat(leaveNotFoundExamples.has("MEETING_PARTICIPANT_NOT_FOUND")).isTrue();
        assertThat(nicknameNotFoundExamples.has("MEETING_NOT_FOUND")).isTrue();
        assertThat(nicknameNotFoundExamples.has("MEETING_PARTICIPANT_NOT_FOUND")).isTrue();

        var guestExamples = paths.path("/api/meetings/invitations/{inviteCode}/guests")
                .path("post").path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var memberExamples = paths.path("/api/meetings/invitations/{inviteCode}/members")
                .path("post").path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        assertThat(paths.path("/api/meetings/invitations/{inviteCode}/guests").path("post").path("description").asText())
                .contains("출발지 이름 공통 안내")
                .doesNotContain("departure.name")
                .doesNotContain("departure.address");
        assertThat(paths.path("/api/meetings/invitations/{inviteCode}/members").path("post").path("description").asText())
                .contains("출발지 이름 공통 안내")
                .doesNotContain("departure.name")
                .doesNotContain("departure.address");
        assertThat(guestExamples.size()).isEqualTo(exampleNames.size());
        assertThat(memberExamples.size()).isEqualTo(exampleNames.size());
        assertThat(exampleNames).allSatisfy(exampleName -> {
            assertThat(guestExamples.has(exampleName)).isTrue();
            assertThat(memberExamples.has(exampleName)).isTrue();
        });
        assertThat(guestExamples.path("PLACE_ONLY").path("value").path("departure").has("name")).isFalse();
        assertThat(memberExamples.path("PLACE_ONLY").path("value").path("departure").has("name")).isFalse();
    }

    @Test
    void swaggerExamplesCanCreateMeetingThenJoinGuestAndDifferentMember() throws Exception {
        List<String> exampleNames = List.of(
                "SCHEDULE_AND_PLACE_DATE_AND_TIME",
                "SCHEDULE_AND_PLACE_DATE_ONLY",
                "SCHEDULE_ONLY_DATE_AND_TIME",
                "SCHEDULE_ONLY_DATE_ONLY",
                "PLACE_ONLY"
        );
        var openApi = objectMapper.readTree(mockMvc.perform(get("/v3/api-docs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());
        var paths = openApi.path("paths");
        var creationExamples = paths.path("/api/meetings").path("post")
                .path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var guestExamples = paths.path("/api/meetings/invitations/{inviteCode}/guests").path("post")
                .path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");
        var memberExamples = paths.path("/api/meetings/invitations/{inviteCode}/members").path("post")
                .path("requestBody").path("content")
                .path(MediaType.APPLICATION_JSON_VALUE).path("examples");

        for (int index = 0; index < exampleNames.size(); index++) {
            String exampleName = exampleNames.get(index);
            String hostToken = signupAndGetAccessToken("swagger-host-" + index, "swagger-host-" + index);
            String memberToken = signupAndGetAccessToken("swagger-member-" + index, "swagger-member-" + index);
            String createResponse = mockMvc.perform(post("/api/meetings")
                            .header("Authorization", "Bearer " + hostToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(creationExamples.path(exampleName).path("value").toString()))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            String inviteCode = objectMapper.readTree(createResponse).path("inviteCode").asText();

            mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(guestExamples.path(exampleName).path("value").toString()))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                            .header("Authorization", "Bearer " + memberToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(memberExamples.path(exampleName).path("value").toString()))
                    .andExpect(status().isCreated());

            if ("SCHEDULE_AND_PLACE_DATE_AND_TIME".equals(exampleName)) {
                mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.availabilityStatuses").isArray());
            }
        }
    }

    @Test
    void dateOnlyParticipantSelectsDatesAndScheduleViewAggregatesByDate() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost-date-view", "host-date-view");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, dateOnlyCreateMeetingRequest());

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "dateguest",
                                "password", "1234",
                                "scheduleResponse", Map.of(
                                        "availableDates", List.of("2026-07-02")
                                )
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist());

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].candidateDate").value("2026-07-02"))
                .andExpect(jsonPath("$.candidates[0].startTime").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].endTime").doesNotExist())
                .andExpect(jsonPath("$.candidates[0].availableParticipantCount").value(2))
                .andExpect(jsonPath("$.candidates[0].availableParticipants.length()").value(2))
                .andExpect(jsonPath("$.candidates.length()").value(1))
                .andExpect(jsonPath("$.availabilityStatuses[1].candidateDate").value("2026-07-02"))
                .andExpect(jsonPath("$.availabilityStatuses[1].availableParticipantCount").value(2));
    }

    @Test
    void getScheduleViewReturnsAtMostFiveMaximumAvailabilityCandidates() throws Exception {
        String hostToken = signupAndGetAccessToken("schedule-five-host", "schedule-five-host");
        List<LocalDate> candidateDates = List.of(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 3),
                LocalDate.of(2026, 7, 4), LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 6)
        );
        CreateMeetingRequest request = new CreateMeetingRequest(
                "schedule-five", "five schedule candidates", 6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY, ScheduleInputType.DATE_ONLY,
                null, null, candidateDates, null, null, 1440
        );
        String inviteCode = createMeetingAndGetInviteCode(hostToken, request);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "fiveguest",
                                "password", "1234",
                                "scheduleResponse", Map.of("availableDates", candidateDates.stream().map(LocalDate::toString).toList())
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates.length()").value(5))
                .andExpect(jsonPath("$.candidates[0].candidateDate").value("2026-07-01"))
                .andExpect(jsonPath("$.candidates[4].candidateDate").value("2026-07-05"));
    }

    @Test
    void dateOnlyScheduleAndPlaceStoresDatesAndDeparturesTogether() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost-date-place", "host-date-place");
        CreateMeetingRequest request = new CreateMeetingRequest(
                "date-place",
                "choose dates and place",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_ONLY,
                null,
                null,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                null,
                new SaveParticipationRequest.DepartureRequest(
                        "host-company",
                        "서울 강남구",
                        BigDecimal.valueOf(37.498095),
                        BigDecimal.valueOf(127.027610),
                        com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                ),
                1440
        );
        String inviteCode = createMeetingAndGetInviteCode(hostToken, request);
        Long meetingId = jdbcTemplate.queryForObject(
                "select id from meetings where invite_code = ?",
                Long.class,
                inviteCode
        );

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "placeguest",
                                "password", "1234",
                                "scheduleResponse", Map.of(
                                        "availableDates", List.of("2026-07-02")
                                ),
                                "departure", Map.of(
                                        "name", "guest-home",
                                        "address", "서울 마포구",
                                        "latitude", 37.566500,
                                        "longitude", 126.978000,
                                        "transportationMode", "CAR"
                                )
                        ))))
                .andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from meeting_participant_schedule_date_availabilities availability
                        join meeting_participants participant on participant.id = availability.participant_id
                        where participant.meeting_id = ?
                        """,
                Long.class,
                meetingId
        )).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from meeting_participant_schedule_availabilities availability
                        join meeting_participants participant on participant.id = availability.participant_id
                        where participant.meeting_id = ?
                        """,
                Long.class,
                meetingId
        )).isZero();
        assertThat(meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId))
                .extracting(participant -> participant.getDepartureName())
                .containsExactly("host-company", "guest-home");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleInputType").value("DATE_ONLY"))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.respondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.responseRate").doesNotExist());
    }

    @Test
    void dateOnlyParticipationRejectsDateOutsideHostCandidates() throws Exception {
        String hostToken = signupAndGetAccessToken("meetinghost-date-invalid", "host-date-invalid");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, dateOnlyCreateMeetingRequest());
        Long meetingId = jdbcTemplate.queryForObject(
                "select id from meetings where invite_code = ?",
                Long.class,
                inviteCode
        );

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "invalid",
                                "password", "1234",
                                "scheduleResponse", Map.of(
                                        "availableDates", List.of("2026-07-03")
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEETING_PARTICIPATION_INPUT"));

        assertThat(meetingParticipantRepository.countByMeetingId(meetingId)).isEqualTo(1L);
    }

    @Test
    void getScheduleViewMergesConsecutiveSlotsForSameParticipants() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost32", "host32", 6);
        Long firstParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-merge-1");
        Long secondParticipantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-merge-2");
        saveDefaultParticipation(inviteCode, firstParticipantId, "09:00", "11:00");
        saveDefaultParticipation(inviteCode, secondParticipantId, List.of(
                scheduleAvailability("09:00", "10:00"),
                scheduleAvailability("10:00", "11:00")
        ));

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LONGEST_MEETING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.candidates[0].endTime").value("11:00:00"))
                .andExpect(jsonPath("$.candidates[0].availableParticipantCount").value(3));
    }

    @Test
    void getScheduleViewRejectsUnsupportedSort() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost33", "host33", 6);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/schedules", inviteCode)
                        .param("sort", "LATEST_DATE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
    }

    @Test
    void getPlaceViewReturnsStraightLineCommercialAreaPreview() throws Exception {
        String inviteCode = createMeetingAndGetInviteCode("meetinghost31", "host31", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-place-view");
        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.recommendationBasis").value("STRAIGHT_LINE_PREVIEW"))
                .andExpect(jsonPath("$.center.latitude").value(37.498095))
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.departureRespondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.participants[0].departureResponded").doesNotExist())
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[0].areaName").isString())
                .andExpect(jsonPath("$.recommendations[0].categoryName").value(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("발달상권"),
                        org.hamcrest.Matchers.is("관광특구")
                )))
                .andExpect(jsonPath("$.recommendations.length()").value(3))
                .andExpect(jsonPath("$.recommendations[0].averageStraightDistanceMeters").isNumber())
                .andExpect(jsonPath("$.recommendations[0].areaCode").value("3120189"))
                .andExpect(jsonPath("$.recommendations[0].station.name").value("강남역"))
                .andExpect(jsonPath("$.recommendations[0].station.name").isString())
                .andExpect(jsonPath("$.recommendations[0].station.lineNames").value(
                        org.hamcrest.Matchers.containsInAnyOrder("2호선", "신분당선")
                ))
                .andExpect(jsonPath("$.recommendations[0].subwayStations").doesNotExist());
    }

    @Test
    void getPlaceViewReturnsNullStationForCommercialAreaWithoutStationMapping() throws Exception {
        String hostToken = signupAndGetAccessToken("place-view-no-station-host", "no-station-host");
        CreateMeetingRequest request = new CreateMeetingRequest(
                "nonstationview",
                null,
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                null,
                null,
                new SaveParticipationRequest.DepartureRequest(
                        "host-location",
                        "서울 종로구 종로 1",
                        BigDecimal.valueOf(37.5741047),
                        BigDecimal.valueOf(126.9810953),
                        com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                ),
                1440
        );
        String inviteCode = createMeetingAndGetInviteCode(hostToken, request);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].areaCode").value("3120004"))
                .andExpect(jsonPath("$.recommendations[0].station").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getPlaceViewReturnsCoordinatesPendingWhenNoDepartureCoordinatesExist() throws Exception {
        CreateMeetingRequest request = new CreateMeetingRequest(
                "coord-pending",
                null,
                6,
                com.moyeo.domain.meeting.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                null,
                null,
                new SaveParticipationRequest.DepartureRequest(
                        "company",
                        "서울 강남구",
                        null,
                        null,
                        com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                ),
                1440
        );
        String accessToken = signupAndGetAccessToken("meetinghost-coordinate-pending", "host-coordinate-pending");
        String inviteCode = createMeetingAndGetInviteCode(accessToken, request);

        mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.recommendationBasis").value("COORDINATES_PENDING"))
                .andExpect(jsonPath("$.center").doesNotExist())
                .andExpect(jsonPath("$.departureRespondedParticipantCount").doesNotExist())
                .andExpect(jsonPath("$.recommendations").isEmpty());
    }

    @Test
    void hostCanCalculateActualTravelTimeRecommendations() throws Exception {
        String accessToken = signupAndGetAccessToken("actual-route-host", "actual-route-host");
        String inviteCode = createMeetingAndGetInviteCode(accessToken, defaultCreateMeetingRequest(6));
        when(kakaoRouteClient.findShortestTravelTimeSeconds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(1_200L);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/view/places/actual-time", inviteCode)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").isNumber())
                .andExpect(jsonPath("$.recommendations.length()").value(3))
                .andExpect(jsonPath("$.recommendations[0].rank").value(1))
                .andExpect(jsonPath("$.recommendations[0].averageTravelTimeSeconds").value(1200));
    }

    @Test
    void nonHostCannotCalculateActualTravelTimeRecommendations() throws Exception {
        String hostToken = signupAndGetAccessToken("actual-route-owner", "actual-route-owner");
        String otherToken = signupAndGetAccessToken("actual-route-other", "actual-route-other");
        String inviteCode = createMeetingAndGetInviteCode(hostToken, defaultCreateMeetingRequest(6));

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/view/places/actual-time", inviteCode)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACTUAL_ROUTE_RECOMMENDATION_FORBIDDEN"));
    }

    @Test
    void actualTravelTimeCalculationRequiresEveryParticipantDeparture() throws Exception {
        String token = signupAndGetAccessToken("actual-route-pending", "actual-route-pending");
        String inviteCode = createMeetingAndGetInviteCode(token, defaultCreateMeetingRequest(6));
        jdbcTemplate.update("update meeting_participants set departure_latitude = null where meeting_id = (select id from meetings where invite_code = ?)", inviteCode);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/view/places/actual-time", inviteCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTUAL_ROUTE_RECOMMENDATION_NOT_READY"));
    }

    @Test
    void actualTravelTimeCalculationAppliesMeetingCooldown() throws Exception {
        String token = signupAndGetAccessToken("actual-route-cooldown", "actual-route-cooldown");
        String inviteCode = createMeetingAndGetInviteCode(token, defaultCreateMeetingRequest(6));
        when(kakaoRouteClient.findShortestTravelTimeSeconds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        )).thenReturn(1_200L);

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/view/places/actual-time", inviteCode)
                        .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/view/places/actual-time", inviteCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ACTUAL_ROUTE_RECOMMENDATION_COOLDOWN"));
    }

    @Test
    void actualTravelTimeCalculationFailsWhenKakaoRouteLookupFails() throws Exception {
        String token = signupAndGetAccessToken("actual-route-unavailable", "actual-route-unavailable");
        String inviteCode = createMeetingAndGetInviteCode(token, defaultCreateMeetingRequest(6));
        when(kakaoRouteClient.findShortestTravelTimeSeconds(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        )).thenThrow(new com.moyeo.route.KakaoRouteUnavailableException(null));

        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/view/places/actual-time", inviteCode)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ACTUAL_ROUTE_RECOMMENDATION_UNAVAILABLE"));
    }

    private String signupAndGetAccessToken(String loginId, String nickname) throws Exception {
        return testMemberFactory.createAccessToken(nickname);
    }

    private String createMeetingAndGetInviteCode(String loginId, String nickname, int maxParticipants) throws Exception {
        return createMeetingAndGetInviteCode(loginId, nickname, defaultCreateMeetingRequest(maxParticipants));
    }

    private String createMeetingAndGetInviteCode(String loginId, String nickname, CreateMeetingRequest request) throws Exception {
        String accessToken = signupAndGetAccessToken(loginId, nickname);
        return createMeetingAndGetInviteCode(accessToken, request);
    }

    private String createMeetingAndGetInviteCode(String accessToken, CreateMeetingRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("inviteCode").asText();
    }

    private Long getMeetingId(String inviteCode) throws Exception {
        String response = mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view", inviteCode))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("meetingId").asLong();
    }

    private void confirmMeeting(Long meetingId, String inviteCode, String hostToken) throws Exception {
        mockMvc.perform(post("/api/meetings/{meetingId}/schedule-confirmation", meetingId)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleDate\":\"2026-07-01\",\"startTime\":\"09:00\",\"endTime\":\"10:00\"}"))
                .andExpect(status().isOk());

        String placeView = mockMvc.perform(get("/api/meetings/invitations/{inviteCode}/view/places", inviteCode))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String commercialAreaCode = objectMapper.readTree(placeView).get("recommendations").get(0).get("areaCode").asText();
        mockMvc.perform(post("/api/meetings/{meetingId}/place-confirmation", meetingId)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("commercialAreaCode", commercialAreaCode))))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("select meeting_status from meetings where id = ?", String.class, meetingId))
                .isEqualTo("CONFIRMED");
    }

    private void joinDateOnlyMember(String inviteCode, String accessToken, String nickname) throws Exception {
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", nickname,
                                "scheduleResponse", Map.of("availableDates", List.of("2026-07-01"))
                        ))))
                .andExpect(status().isCreated());
    }

    private void confirmDateOnlyMeeting(Long meetingId, String hostToken) throws Exception {
        mockMvc.perform(post("/api/meetings/{meetingId}/schedule-confirmation", meetingId)
                        .header("Authorization", "Bearer " + hostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduleDate\":\"2026-07-01\"}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("select meeting_status from meetings where id = ?", String.class, meetingId))
                .isEqualTo("CONFIRMED");
    }

    private long countRows(String tableName, String foreignKeyColumn, Long id) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + tableName + " where " + foreignKeyColumn + " = ?",
                Long.class,
                id
        );
    }

    private Long createMeetingAndGetMeetingId(String accessToken, CreateMeetingRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/meetings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("meetingId").asLong();
    }

    private CreateMeetingRequest createMeetingRequest(
            String name,
            String description,
            int maxParticipants,
            com.moyeo.domain.meeting.PlanningType planningType,
            ScheduleInputType scheduleInputType,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            int deadlineMinutes
    ) {
        boolean requiresSchedule = planningType == com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY
                || planningType == com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE;
        boolean requiresPlace = planningType == com.moyeo.domain.meeting.PlanningType.PLACE_ONLY
                || planningType == com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE;
        List<LocalDate> candidateDates = requiresSchedule
                ? List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2))
                : null;
        SaveParticipationRequest.ScheduleResponseRequest scheduleResponse =
                scheduleInputType == ScheduleInputType.DATE_AND_TIME
                        && availableStartTime != null
                        && availableEndTime != null
                        && availableStartTime.isBefore(availableEndTime)
                        ? new SaveParticipationRequest.ScheduleResponseRequest(
                                null,
                                List.of(new SaveParticipationRequest.ScheduleAvailabilityRequest(
                                        LocalDate.of(2026, 7, 1),
                                        availableStartTime,
                                        availableStartTime.plusHours(2).isAfter(availableEndTime)
                                                ? availableEndTime
                                                : availableStartTime.plusHours(2)
                                ))
                        )
                        : null;
        SaveParticipationRequest.DepartureRequest departure = requiresPlace
                ? new SaveParticipationRequest.DepartureRequest(
                        "company",
                        "서울 강남구",
                        BigDecimal.valueOf(37.498095),
                        BigDecimal.valueOf(127.027610),
                        com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                )
                : null;
        return new CreateMeetingRequest(
                name,
                description,
                maxParticipants,
                planningType,
                scheduleInputType,
                availableStartTime,
                availableEndTime,
                candidateDates,
                scheduleResponse,
                departure,
                deadlineMinutes
        );
    }

    private CreateMeetingRequest defaultCreateMeetingRequest(int maxParticipants) {
        return createMeetingRequest(
                "weekend-meeting",
                "dinner together",
                maxParticipants,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                1440
        );
    }

    private CreateMeetingRequest invalidCreateMeetingRequest() {
        return createMeetingRequest(
                "",
                "x".repeat(101),
                1,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_AND_PLACE,
                ScheduleInputType.DATE_AND_TIME,
                LocalTime.of(18, 0),
                LocalTime.of(9, 0),
                0
        );
    }

    private CreateMeetingRequest dateOnlyCreateMeetingRequest() {
        return createMeetingRequest(
                "date-only",
                "choose available dates",
                6,
                com.moyeo.domain.meeting.PlanningType.SCHEDULE_ONLY,
                ScheduleInputType.DATE_ONLY,
                null,
                null,
                1440
        );
    }

    private void joinGuest(String inviteCode, String nickname) throws Exception {
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest(normalizeGuestNickname(nickname), "1234"))))
                .andExpect(status().isCreated());
    }

    private void joinMember(String inviteCode, String accessToken, String nickname) throws Exception {
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/members", inviteCode)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultMemberJoinRequest(nickname))))
                .andExpect(status().isCreated());
    }

    private Long joinGuestAndGetParticipantId(String inviteCode, String nickname) throws Exception {
        String response = mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultGuestJoinRequest(normalizeGuestNickname(nickname), "1234"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("participantId").asLong();
    }

    private void joinScheduleOnlyGuest(String inviteCode, String nickname, String startTime, String endTime) throws Exception {
        mockMvc.perform(post("/api/meetings/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", nickname,
                                "password", "1234",
                                "scheduleResponse", Map.of(
                                        "availableTimeRanges", List.of(scheduleAvailability(startTime, endTime))
                                )
                        ))))
                .andExpect(status().isCreated());
    }

    private void saveDefaultParticipation(String inviteCode, Long participantId, String startTime, String endTime) throws Exception {
        saveDefaultParticipation(inviteCode, participantId, List.of(scheduleAvailability(startTime, endTime)));
    }

    private byte[] pngImage() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xFF0000);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void saveDefaultParticipation(
            String inviteCode,
            Long participantId,
            List<Map<String, String>> scheduleAvailabilities
    ) throws Exception {
        meetingService.saveParticipation(
                inviteCode,
                participantId,
                new SaveParticipationCommand(
                        List.of(),
                        scheduleAvailabilities.stream()
                                .map(slot -> new SaveParticipationCommand.ScheduleAvailability(
                                        LocalDate.parse(slot.get("candidateDate")),
                                        LocalTime.parse(slot.get("startTime")),
                                        LocalTime.parse(slot.get("endTime"))
                                ))
                                .toList(),
                        new SaveParticipationCommand.Departure(
                                "company",
                                "서울 강남구",
                                BigDecimal.valueOf(37.498095),
                                BigDecimal.valueOf(127.027610),
                                com.moyeo.domain.meeting.TransportationMode.PUBLIC_TRANSIT
                        )
                )
        );
    }

    private Map<String, Object> defaultGuestJoinRequest(String nickname, String password) {
        return defaultJoinRequest(nickname, password);
    }

    private String normalizeGuestNickname(String nickname) {
        String normalized = nickname
                .replace('0', 'a')
                .replace('1', 'b')
                .replace('2', 'c')
                .replaceAll("[^A-Za-z]", "");
        return normalized.length() <= 10
                ? normalized
                : normalized.substring(0, 9) + normalized.charAt(normalized.length() - 1);
    }

    private Map<String, Object> defaultMemberJoinRequest(String nickname) {
        return Map.of(
                "nickname", nickname,
                "scheduleResponse", Map.of(
                        "availableTimeRanges", List.of(scheduleAvailability("09:00", "10:00"))
                ),
                "departure", Map.of(
                        "name", "company",
                        "address", "서울 강남구",
                        "latitude", 37.498095,
                        "longitude", 127.027610,
                        "transportationMode", "PUBLIC_TRANSIT"
                )
        );
    }

    private Map<String, Object> defaultJoinRequest(String nickname, String password) {
        return Map.of(
                "nickname", nickname,
                "password", password,
                "scheduleResponse", Map.of(
                        "availableTimeRanges", List.of(scheduleAvailability("09:00", "10:00"))
                ),
                "departure", Map.of(
                        "name", "company",
                        "address", "서울 강남구",
                        "latitude", 37.498095,
                        "longitude", 127.027610,
                        "transportationMode", "PUBLIC_TRANSIT"
                )
        );
    }

    private Map<String, String> scheduleAvailability(String startTime, String endTime) {
        return Map.of(
                "candidateDate", "2026-07-01",
                "startTime", startTime,
                "endTime", endTime
        );
    }
}
