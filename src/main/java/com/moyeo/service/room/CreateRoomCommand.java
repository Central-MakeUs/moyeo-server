package com.moyeo.service.room;

import com.moyeo.domain.room.PlaceMode;
import com.moyeo.domain.room.PlaceRecommendationStrategy;
import com.moyeo.domain.room.ScheduleMode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record CreateRoomCommand(
        String name,
        String description,
        int maxParticipants,
        ScheduleMode scheduleMode,
        LocalDateTime fixedScheduleAt,
        List<LocalDate> scheduleCandidateDates,
        LocalTime availableStartTime,
        LocalTime availableEndTime,
        PlaceMode placeMode,
        PlaceRecommendationStrategy placeRecommendationStrategy,
        String fixedPlaceName,
        String fixedPlaceAddress,
        int deadlineMinutes
) {
}
