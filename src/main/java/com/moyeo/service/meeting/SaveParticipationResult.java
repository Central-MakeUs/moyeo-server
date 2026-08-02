package com.moyeo.service.meeting;

public record SaveParticipationResult(
        Long meetingId,
        Long participantId,
        Long userId,
        int scheduleAvailabilityCount,
        boolean hasDeparture
) {
}
