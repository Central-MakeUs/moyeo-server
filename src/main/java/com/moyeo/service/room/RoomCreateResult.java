package com.moyeo.service.room;

import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;
import com.moyeo.domain.room.RoomScheduleCandidate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.List;

public record RoomCreateResult(
        Long roomId,
        String name,
        String description,
        Integer maxParticipants,
        String planningType,
        String scheduleMode,
        List<LocalDate> scheduleCandidateDates,
        LocalTime availableStartTime,
        LocalTime availableEndTime,
        String placeMode,
        String placeRecommendationStrategy,
        LocalDateTime deadlineAt,
        String inviteCode,
        String invitePath,
        String hostDepartureName,
        String hostDepartureAddress,
        BigDecimal hostDepartureLatitude,
        BigDecimal hostDepartureLongitude,
        String hostTransportationMode,
        Long hostParticipantId
) {

    public static RoomCreateResult from(
            Room room,
            RoomParticipant hostParticipant,
            List<RoomScheduleCandidate> scheduleCandidates
    ) {
        return new RoomCreateResult(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getMaxParticipants(),
                room.getPlanningType().name(),
                room.getScheduleMode().name(),
                scheduleCandidates.stream().map(RoomScheduleCandidate::getCandidateDate).toList(),
                room.getAvailableStartTime(),
                room.getAvailableEndTime(),
                room.getPlaceMode().name(),
                room.getPlaceRecommendationStrategy() != null ? room.getPlaceRecommendationStrategy().name() : null,
                room.getDeadlineAt(),
                room.getInviteCode(),
                "/rooms/invitations/" + room.getInviteCode(),
                hostParticipant.getDepartureName(),
                hostParticipant.getDepartureAddress(),
                hostParticipant.getDepartureLatitude(),
                hostParticipant.getDepartureLongitude(),
                hostParticipant.getTransportationMode() != null ? hostParticipant.getTransportationMode().name() : null,
                hostParticipant.getId()
        );
    }
}
