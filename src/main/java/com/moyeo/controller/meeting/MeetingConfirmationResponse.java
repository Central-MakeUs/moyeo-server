package com.moyeo.controller.meeting;
import com.moyeo.service.meeting.MeetingConfirmationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
@Schema(description = "일정 또는 장소 확정 결과")
public record MeetingConfirmationResponse(
        @Schema(description = "모임 ID") Long meetingId,
        @Schema(description = "현재 모임 상태", allowableValues = {"PLANNING", "CONFIRMED"}) String status,
        @Schema(description = "일정과 장소가 모두 확정된 최종 확정 시각. 아직 한 항목만 확정됐으면 null") LocalDateTime confirmedAt,
        @Schema(description = "확정 일정 날짜. 아직 일정 확정 전이면 null") LocalDate scheduleDate,
        @Schema(description = "확정 시작 시간. DATE_ONLY면 null") LocalTime startTime,
        @Schema(description = "확정 종료 시간. DATE_ONLY면 null") LocalTime endTime,
        @Schema(description = "확정 장소명. 아직 장소 확정 전이면 null") String placeName) {
    public static MeetingConfirmationResponse from(MeetingConfirmationResult r) { return new MeetingConfirmationResponse(r.meetingId(), r.status(), r.confirmedAt(), r.scheduleDate(), r.startTime(), r.endTime(), r.placeName()); }
}
