package com.moyeo.service.meeting;
import java.time.LocalDate;
import java.time.LocalTime;
public record ConfirmMeetingCommand(LocalDate scheduleDate, LocalTime startTime, LocalTime endTime, String commercialAreaCode) { }
