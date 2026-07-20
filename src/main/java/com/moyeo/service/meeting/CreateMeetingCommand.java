package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.ScheduleInputType;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CreateMeetingCommand(
        String name,
        String description,
        int maxParticipants,
        PlanningType planningType,
        ScheduleMode scheduleMode,
        ScheduleInputType scheduleInputType,
        LocalDateTime fixedScheduleAt,
        LocalTime availableStartTime,
        LocalTime availableEndTime,
        PlaceMode placeMode,
        String fixedPlaceName,
        String fixedPlaceAddress,
        int deadlineMinutes
) {
}
