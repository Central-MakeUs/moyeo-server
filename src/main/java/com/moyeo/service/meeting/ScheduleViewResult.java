package com.moyeo.service.meeting;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleViewResult(
        Long meetingId,
        String scheduleInputType,
        String sort,
        long participantCount,
        List<Candidate> candidates,
        List<AvailabilityStatus> availabilityStatuses
) {

    public record Candidate(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime,
            long availableParticipantCount,
            List<AvailableParticipant> availableParticipants
    ) {
    }

    public record AvailabilityStatus(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime,
            long availableParticipantCount
    ) {
    }

    public record AvailableParticipant(Long participantId, Long userId, String nickname) {
    }
}
