package com.moyeo.service.meeting;

import java.time.LocalDate;
import java.time.LocalTime;

public record ConfirmScheduleCommand(LocalDate scheduleDate, LocalTime startTime, LocalTime endTime) { }
