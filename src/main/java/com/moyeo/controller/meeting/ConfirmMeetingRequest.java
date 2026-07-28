package com.moyeo.controller.meeting;
import com.moyeo.service.meeting.ConfirmMeetingCommand;
import java.time.LocalDate;
import java.time.LocalTime;
public record ConfirmMeetingRequest(LocalDate scheduleDate, LocalTime startTime, LocalTime endTime, String commercialAreaCode) {
    public ConfirmMeetingCommand toCommand() { return new ConfirmMeetingCommand(scheduleDate, startTime, endTime, commercialAreaCode); }
}
