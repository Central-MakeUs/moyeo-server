package com.moyeo.service.room;

public record SaveParticipationResult(
        Long roomId,
        Long participantId,
        int scheduleAvailabilityCount,
        boolean hasDeparture
) {
}
