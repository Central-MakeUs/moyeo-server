package com.moyeo.controller.room;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyeo.repository.room.RoomParticipantRepository;
import com.moyeo.repository.room.RoomParticipantScheduleAvailabilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Autowired
    private RoomParticipantScheduleAvailabilityRepository roomParticipantScheduleAvailabilityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createRoomReturnsInviteCodeAndHostParticipant() throws Exception {
        String accessToken = signupAndGetAccessToken("roomhost1", "방장1");

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultCreateRoomRequest(6))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.roomId").isNumber())
                .andExpect(jsonPath("$.name").value("토요일 모임"))
                .andExpect(jsonPath("$.description").value("같이 저녁 먹어요."))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.planningType").value("SCHEDULE_AND_PLACE"))
                .andExpect(jsonPath("$.scheduleMode").value("VOTE"))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.availableStartTime").value("09:00:00"))
                .andExpect(jsonPath("$.availableEndTime").value("18:00:00"))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.deadlineAt").isString())
                .andExpect(jsonPath("$.inviteCode").isString())
                .andExpect(jsonPath("$.invitePath").isString())
                .andExpect(jsonPath("$.hostDepartureName").value("회사"))
                .andExpect(jsonPath("$.hostDepartureAddress").value("Seoul Gangnam"))
                .andExpect(jsonPath("$.hostDepartureLatitude").value(37.498095))
                .andExpect(jsonPath("$.hostDepartureLongitude").value(127.027610))
                .andExpect(jsonPath("$.hostTransportationMode").value("PUBLIC_TRANSIT"))
                .andExpect(jsonPath("$.hostParticipantId").isNumber());
    }

    @Test
    void createRoomRequiresAccessToken() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "토요일 모임",
                                "maxParticipants", 6
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void createRoomValidatesRequest() throws Exception {
        String accessToken = signupAndGetAccessToken("roomhost2", "방장2");

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCreateRoomRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createRoomRejectsNonHourUnitScheduleTime() throws Exception {
        String accessToken = signupAndGetAccessToken("roomhost10", "host10");

        CreateRoomRequest request = new CreateRoomRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.room.PlanningType.SCHEDULE_ONLY,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                LocalTime.of(18, 30),
                LocalTime.of(22, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                1440
        );

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createRoomRejectsNonTenMinuteDeadline() throws Exception {
        String accessToken = signupAndGetAccessToken("roomhost11", "host11");

        CreateRoomRequest request = new CreateRoomRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.room.PlanningType.SCHEDULE_AND_PLACE,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                com.moyeo.domain.room.PlaceRecommendationStrategy.MIDDLE_POINT,
                "회사",
                "Seoul Gangnam",
                BigDecimal.valueOf(37.498095),
                BigDecimal.valueOf(127.027610),
                com.moyeo.domain.room.TransportationMode.PUBLIC_TRANSIT,
                15
        );

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createRoomRejectsMiddlePointWithoutHostDepartureSnapshot() throws Exception {
        String accessToken = signupAndGetAccessToken("roomhost17", "host17");

        CreateRoomRequest request = new CreateRoomRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.room.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                com.moyeo.domain.room.PlaceRecommendationStrategy.MIDDLE_POINT,
                null,
                "Seoul Gangnam",
                BigDecimal.valueOf(37.498095),
                BigDecimal.valueOf(127.027610),
                com.moyeo.domain.room.TransportationMode.PUBLIC_TRANSIT,
                1440
        );

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void createRoomSupportsPlaceOnlyPlanning() throws Exception {
        String accessToken = signupAndGetAccessToken("roomhost13", "host13");

        CreateRoomRequest request = new CreateRoomRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.room.PlanningType.PLACE_ONLY,
                null,
                null,
                null,
                com.moyeo.domain.room.PlaceRecommendationStrategy.RANDOM,
                null,
                null,
                null,
                null,
                null,
                1440
        );

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planningType").value("PLACE_ONLY"))
                .andExpect(jsonPath("$.scheduleMode").value("NONE"))
                .andExpect(jsonPath("$.scheduleCandidateDates").isEmpty())
                .andExpect(jsonPath("$.availableStartTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.availableEndTime").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("RANDOM"));
    }

    @Test
    void createRoomRemovesDuplicatedScheduleCandidateDatesAndSortsThem() throws Exception {
        String accessToken = signupAndGetAccessToken("roomhost14", "host14");

        CreateRoomRequest request = new CreateRoomRequest(
                "weekend",
                "dinner",
                6,
                com.moyeo.domain.room.PlanningType.SCHEDULE_ONLY,
                List.of(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)),
                LocalTime.of(18, 0),
                LocalTime.of(22, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                1440
        );

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleCandidateDates.length()").value(2))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.scheduleCandidateDates[1]").value("2026-07-02"));
    }

    @Test
    void getInvitationReturnsRoomInfo() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost3", "방장3", 6);

        mockMvc.perform(get("/api/rooms/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.roomId").isNumber())
                .andExpect(jsonPath("$.name").value("토요일 모임"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.planningType").value("SCHEDULE_AND_PLACE"))
                .andExpect(jsonPath("$.scheduleMode").value("VOTE"))
                .andExpect(jsonPath("$.scheduleCandidateDates[0]").value("2026-07-01"))
                .andExpect(jsonPath("$.placeMode").value("RECOMMEND"))
                .andExpect(jsonPath("$.placeRecommendationStrategy").value("MIDDLE_POINT"))
                .andExpect(jsonPath("$.deadlineAt").isString())
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.hostNickname").value("방장3"))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(true))
                .andExpect(jsonPath("$.participationStatus.reason").value("AVAILABLE"))
                .andExpect(jsonPath("$.participationStatus.message").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void getInvitationRejectsUnknownInviteCode() throws Exception {
        mockMvc.perform(get("/api/rooms/invitations/{inviteCode}", "UNKNOWN123"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ROOM_INVITATION_NOT_FOUND"));
    }

    @Test
    void getInvitationReturnsParticipantLimitExceededStatus() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost15", "host15", 2);
        joinGuest(inviteCode, "guest-limit");

        mockMvc.perform(get("/api/rooms/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("PARTICIPANT_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.participationStatus.message").value("모인 인원이 모두 찼어요. 아쉽지만 현재는 더 이상 참여할 수 없어요."));
    }

    @Test
    void getInvitationReturnsDeadlinePassedStatusBeforeParticipantLimitStatus() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost16", "host16", 2);
        joinGuest(inviteCode, "guest-deadline-status");
        jdbcTemplate.update("update rooms set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?", inviteCode);

        mockMvc.perform(get("/api/rooms/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2))
                .andExpect(jsonPath("$.participationStatus.canJoin").value(false))
                .andExpect(jsonPath("$.participationStatus.reason").value("DEADLINE_PASSED"))
                .andExpect(jsonPath("$.participationStatus.message").value("기한이 지난 모임이에요. 아쉽지만 현재는 더 이상 참여할 수 없어요."));
    }

    @Test
    void joinGuestCreatesGuestParticipant() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost4", "방장4", 6);

        String response = mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "게스트",
                                "password", "guestpass123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.roomId").isNumber())
                .andExpect(jsonPath("$.participantId").isNumber())
                .andExpect(jsonPath("$.nickname").value("게스트"))
                .andExpect(jsonPath("$.participantType").value("GUEST"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long participantId = objectMapper.readTree(response).get("participantId").asLong();
        var participant = roomParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getPasswordHash()).isNotEqualTo("guestpass123");
        assertThat(passwordEncoder.matches("guestpass123", participant.getPasswordHash())).isTrue();
    }

    @Test
    void joinGuestRejectsDuplicatedNicknameInSameRoom() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost5", "방장5", 6);
        joinGuest(inviteCode, "게스트1");

        mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "게스트1",
                                "password", "guestpass123"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_ROOM_PARTICIPANT_NICKNAME"));
    }

    @Test
    void joinGuestAllowsMultipleGuestsWithNullUserId() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost9", "방장9", 6);

        joinGuest(inviteCode, "게스트1");
        joinGuest(inviteCode, "게스트2");

        mockMvc.perform(get("/api/rooms/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    void joinGuestRejectsHostNicknameInSameRoom() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost6", "현우", 6);

        mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "현우",
                                "password", "guestpass123"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DUPLICATE_ROOM_PARTICIPANT_NICKNAME"));
    }

    @Test
    void joinGuestRejectsExceededParticipantLimit() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost7", "방장7", 2);
        joinGuest(inviteCode, "게스트1");

        mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "게스트2",
                                "password", "guestpass123"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ROOM_PARTICIPANT_LIMIT_EXCEEDED"));
    }

    @Test
    void joinGuestRejectsClosedRoomParticipation() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost12", "host12", 6);
        jdbcTemplate.update("update rooms set deadline_at = dateadd('second', -1, current_timestamp) where invite_code = ?", inviteCode);

        mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", "guest-deadline",
                                "password", "guestpass123"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ROOM_PARTICIPATION_CLOSED"));
    }

    @Test
    void joinGuestValidatesRequest() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost8", "방장8", 6);

        mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
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
    void saveParticipationStoresScheduleAvailabilitiesAndDeparture() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost18", "host18", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-participation");

        mockMvc.perform(put("/api/rooms/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleAvailabilities", List.of(
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
                                ),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantId").value(participantId))
                .andExpect(jsonPath("$.scheduleAvailabilityCount").value(2))
                .andExpect(jsonPath("$.hasDeparture").value(true));

        var participant = roomParticipantRepository.findById(participantId).orElseThrow();
        assertThat(participant.getDepartureName()).isEqualTo("company");
        assertThat(participant.getDepartureAddress()).isEqualTo("Seoul Gangnam");
        assertThat(participant.getTransportationMode()).isEqualTo(com.moyeo.domain.room.TransportationMode.PUBLIC_TRANSIT);
        assertThat(roomParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(2);
    }

    @Test
    void saveParticipationReplacesPreviousScheduleAvailabilities() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost19", "host19", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-replace");

        saveDefaultParticipation(inviteCode, participantId, "09:00", "10:00");
        saveDefaultParticipation(inviteCode, participantId, "11:00", "12:00");

        assertThat(roomParticipantScheduleAvailabilityRepository.countByParticipantId(participantId)).isEqualTo(1);
    }

    @Test
    void saveParticipationRejectsOutOfRangeScheduleAvailability() throws Exception {
        String inviteCode = createRoomAndGetInviteCode("roomhost20", "host20", 6);
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-invalid-time");

        mockMvc.perform(put("/api/rooms/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleAvailabilities", List.of(Map.of(
                                        "candidateDate", "2026-07-01",
                                        "startTime", "08:00",
                                        "endTime", "09:00"
                                )),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_PARTICIPATION_INPUT"));
    }

    @Test
    void saveParticipationRejectsDepartureForScheduleOnlyRoom() throws Exception {
        String inviteCode = createRoomAndGetInviteCode(
                "roomhost21",
                "host21",
                new CreateRoomRequest(
                        "schedule",
                        "schedule only",
                        6,
                        com.moyeo.domain.room.PlanningType.SCHEDULE_ONLY,
                        List.of(LocalDate.of(2026, 7, 1)),
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1440
                )
        );
        Long participantId = joinGuestAndGetParticipantId(inviteCode, "guest-schedule-only");

        mockMvc.perform(put("/api/rooms/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleAvailabilities", List.of(Map.of(
                                        "candidateDate", "2026-07-01",
                                        "startTime", "09:00",
                                        "endTime", "10:00"
                                )),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_PARTICIPATION_INPUT"));
    }

    private String signupAndGetAccessToken(String loginId, String nickname) throws Exception {
        String response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "loginId", loginId,
                                "password", "password123!",
                                "nickname", nickname
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createRoomAndGetInviteCode(String loginId, String nickname, int maxParticipants) throws Exception {
        return createRoomAndGetInviteCode(loginId, nickname, defaultCreateRoomRequest(maxParticipants));
    }

    private String createRoomAndGetInviteCode(String loginId, String nickname, CreateRoomRequest request) throws Exception {
        String accessToken = signupAndGetAccessToken(loginId, nickname);
        String response = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("inviteCode").asText();
    }

    private CreateRoomRequest defaultCreateRoomRequest(int maxParticipants) {
        return new CreateRoomRequest(
                "토요일 모임",
                "같이 저녁 먹어요.",
                maxParticipants,
                com.moyeo.domain.room.PlanningType.SCHEDULE_AND_PLACE,
                List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2)),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                com.moyeo.domain.room.PlaceRecommendationStrategy.MIDDLE_POINT,
                "회사",
                "Seoul Gangnam",
                BigDecimal.valueOf(37.498095),
                BigDecimal.valueOf(127.027610),
                com.moyeo.domain.room.TransportationMode.PUBLIC_TRANSIT,
                1440
        );
    }

    private CreateRoomRequest invalidCreateRoomRequest() {
        return new CreateRoomRequest(
                "",
                "x".repeat(101),
                1,
                com.moyeo.domain.room.PlanningType.SCHEDULE_AND_PLACE,
                List.of(),
                LocalTime.of(18, 0),
                LocalTime.of(9, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                0
        );
    }

    private void joinGuest(String inviteCode, String nickname) throws Exception {
        mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", nickname,
                                "password", "guestpass123"
                        ))))
                .andExpect(status().isCreated());
    }

    private Long joinGuestAndGetParticipantId(String inviteCode, String nickname) throws Exception {
        String response = mockMvc.perform(post("/api/rooms/invitations/{inviteCode}/guests", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "nickname", nickname,
                                "password", "guestpass123"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("participantId").asLong();
    }

    private void saveDefaultParticipation(String inviteCode, Long participantId, String startTime, String endTime) throws Exception {
        mockMvc.perform(put("/api/rooms/invitations/{inviteCode}/participants/{participantId}/participation", inviteCode, participantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "scheduleAvailabilities", List.of(Map.of(
                                        "candidateDate", "2026-07-01",
                                        "startTime", startTime,
                                        "endTime", endTime
                                )),
                                "departure", Map.of(
                                        "name", "company",
                                        "address", "Seoul Gangnam",
                                        "latitude", 37.498095,
                                        "longitude", 127.027610,
                                        "transportationMode", "PUBLIC_TRANSIT"
                                )
                        ))))
                .andExpect(status().isOk());
    }
}
