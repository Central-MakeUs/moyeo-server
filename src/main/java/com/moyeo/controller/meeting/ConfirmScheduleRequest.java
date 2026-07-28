package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ConfirmScheduleCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "일정 확정 요청")
public record ConfirmScheduleRequest(
        @Schema(description = "생성 시 등록한 후보 날짜 중 선택할 날짜", example = "2026-07-10", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate scheduleDate,
        @Schema(description = "DATE_AND_TIME에서만 필수. DATE_ONLY에서는 생략", example = "18:00:00") LocalTime startTime,
        @Schema(description = "DATE_AND_TIME에서만 필수. DATE_ONLY에서는 생략", example = "20:00:00") LocalTime endTime) {
    public ConfirmScheduleCommand toCommand() { return new ConfirmScheduleCommand(scheduleDate, startTime, endTime); }
}
