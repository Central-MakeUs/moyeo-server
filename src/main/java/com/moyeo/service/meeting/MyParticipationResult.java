package com.moyeo.service.meeting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MyParticipationResult(
        Long meetingId,
        String participantType,
        String scheduleInputType,
        ScheduleResponse scheduleResponse,
        Departure departure
) {

    public record ScheduleResponse(
            List<LocalDate> availableDates,
            List<ScheduleAvailability> availableTimeRanges
    ) {
    }

    public record ScheduleAvailability(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    public record Departure(
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String transportationMode
    ) {
    }
}
