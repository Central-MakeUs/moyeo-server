package com.moyeo.service.meeting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MeetingViewResult(
        Long meetingId,
        String name,
        String description,
        String coverImageUrl,
        String planningType,
        boolean meetingConfirmed,
        String scheduleMode,
        String scheduleInputType,
        String placeMode,
        String placeRecommendationStrategy,
        int maxParticipants,
        long participantCount,
        LocalDateTime deadlineAt,
        Long remainingMinutes,
        LocalDate confirmedScheduleDate,
        LocalTime confirmedStartTime,
        LocalTime confirmedEndTime,
        String confirmedPlaceName,
        List<Participant> participants
) {

    public record Participant(
            Long participantId,
            Long userId,
            String nickname,
            String participantType,
            boolean withdrawn
    ) {
    }
}
