package com.moyeo.controller.meeting;
import com.moyeo.service.meeting.MeetingConfirmationResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
public record MeetingConfirmationResponse(Long meetingId, String status, LocalDateTime confirmedAt, LocalDate scheduleDate, LocalTime startTime, LocalTime endTime, String placeName) {
    public static MeetingConfirmationResponse from(MeetingConfirmationResult r) { return new MeetingConfirmationResponse(r.meetingId(), r.status(), r.confirmedAt(), r.scheduleDate(), r.startTime(), r.endTime(), r.placeName()); }
}
