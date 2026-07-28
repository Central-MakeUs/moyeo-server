package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ConfirmScheduleCommand;
import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmScheduleRequest(LocalDate scheduleDate, LocalTime startTime, LocalTime endTime) {
    public ConfirmScheduleCommand toCommand() { return new ConfirmScheduleCommand(scheduleDate, startTime, endTime); }
}
