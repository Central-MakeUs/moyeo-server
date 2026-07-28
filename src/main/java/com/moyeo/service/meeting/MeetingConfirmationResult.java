package com.moyeo.service.meeting;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
public record MeetingConfirmationResult(Long meetingId, String status, LocalDateTime confirmedAt, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime, String placeName) { }
