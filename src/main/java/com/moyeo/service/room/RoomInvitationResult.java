package com.moyeo.service.room;

import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomScheduleCandidate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record RoomInvitationResult(
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
        long participantCount,
        String hostNickname,
        ParticipationStatus participationStatus
) {

    public static RoomInvitationResult from(
            Room room,
            long participantCount,
            List<RoomScheduleCandidate> scheduleCandidates
    ) {
        ParticipationStatus participationStatus = ParticipationStatus.from(room, participantCount);
        return new RoomInvitationResult(
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
                participantCount,
                room.getHostUser().getNickname(),
                participationStatus
        );
    }

    public record ParticipationStatus(
            boolean canJoin,
            String reason,
            String message
    ) {

        private static final String AVAILABLE = "AVAILABLE";
        private static final String DEADLINE_PASSED = "DEADLINE_PASSED";
        private static final String PARTICIPANT_LIMIT_EXCEEDED = "PARTICIPANT_LIMIT_EXCEEDED";
        private static final String DEADLINE_PASSED_MESSAGE = "\uAE30\uD55C\uC774 \uC9C0\uB09C \uBAA8\uC784\uC774\uC5D0\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694.";
        private static final String PARTICIPANT_LIMIT_EXCEEDED_MESSAGE = "\uBAA8\uC778 \uC778\uC6D0\uC774 \uBAA8\uB450 \uCC3C\uC5B4\uC694. \uC544\uC27D\uC9C0\uB9CC \uD604\uC7AC\uB294 \uB354 \uC774\uC0C1 \uCC38\uC5EC\uD560 \uC218 \uC5C6\uC5B4\uC694.";

        private static ParticipationStatus from(Room room, long participantCount) {
            if (!room.getDeadlineAt().isAfter(LocalDateTime.now())) {
                return new ParticipationStatus(false, DEADLINE_PASSED, DEADLINE_PASSED_MESSAGE);
            }

            if (participantCount >= room.getMaxParticipants()) {
                return new ParticipationStatus(false, PARTICIPANT_LIMIT_EXCEEDED, PARTICIPANT_LIMIT_EXCEEDED_MESSAGE);
            }

            return new ParticipationStatus(true, AVAILABLE, null);
        }
    }
}
