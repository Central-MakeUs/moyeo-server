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
        private static final String DEADLINE_PASSED_MESSAGE = "기한이 지난 모임이에요. 아쉽지만 현재는 더 이상 참여할 수 없어요.";
        private static final String PARTICIPANT_LIMIT_EXCEEDED_MESSAGE = "모인 인원이 모두 찼어요. 아쉽지만 현재는 더 이상 참여할 수 없어요.";

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
