package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MyParticipationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "방장, 회원 또는 게스트의 모임 참여 응답")
public record MyParticipationResponse(
        @Schema(description = "모임 ID", example = "17") Long meetingId,
        @Schema(description = "현재 사용자의 모임 역할", allowableValues = {"HOST", "MEMBER", "GUEST"}) String participantType,
        @Schema(description = "일정 입력 유형", allowableValues = {"DATE_ONLY", "DATE_AND_TIME", "NONE"}) String scheduleInputType,
        @Schema(description = "본인의 일정 응답입니다. DATE_ONLY에서는 availableDates만 사용하고 availableTimeRanges는 빈 배열입니다. DATE_AND_TIME에서는 availableTimeRanges만 사용하고 availableDates는 빈 배열입니다. 일정 조율이 없는 모임(NONE)에서는 null입니다.", nullable = true)
        ScheduleResponse scheduleResponse,
        @Schema(description = "본인의 출발지와 교통수단 응답입니다. 장소 조율 모임에서는 값이 있으며, 장소 조율이 없는 모임에서는 null입니다.", nullable = true)
        Departure departure
) {

    static MyParticipationResponse from(MyParticipationResult result) {
        return new MyParticipationResponse(
                result.meetingId(),
                result.participantType(),
                result.scheduleInputType(),
                result.scheduleResponse() == null ? null : new ScheduleResponse(
                        result.scheduleResponse().availableDates(),
                        result.scheduleResponse().availableTimeRanges().stream()
                                .map(slot -> new ScheduleAvailability(slot.candidateDate(), slot.startTime(), slot.endTime()))
                                .toList()
                ),
                result.departure() == null ? null : new Departure(
                        result.departure().name(), result.departure().address(), result.departure().latitude(),
                        result.departure().longitude(), result.departure().transportationMode()
                )
        );
    }

    public record ScheduleResponse(
            @Schema(description = "DATE_ONLY 모임에서 선택한 후보 날짜 목록입니다. DATE_AND_TIME 모임에서는 빈 배열입니다.", example = "[\"2026-07-10\", \"2026-07-12\"]")
            List<LocalDate> availableDates,
            @Schema(description = "DATE_AND_TIME 모임에서 선택한 가능 시간 범위 목록입니다. DATE_ONLY 모임에서는 빈 배열입니다.")
            List<ScheduleAvailability> availableTimeRanges
    ) {
    }

    public record ScheduleAvailability(
            @Schema(description = "모임 후보 날짜", example = "2026-07-10") LocalDate candidateDate,
            @Schema(description = "가능 시작 시간", example = "18:00") LocalTime startTime,
            @Schema(description = "가능 종료 시간", example = "20:00") LocalTime endTime
    ) {
    }

    public record Departure(
            @Schema(description = "출발지 표시 이름입니다. 입력하지 않았다면 null입니다.", nullable = true, example = "회사") String name,
            @Schema(description = "출발지 주소", example = "서울 강남구 테헤란로 123") String address,
            @Schema(description = "출발지 위도", nullable = true, example = "37.498095") BigDecimal latitude,
            @Schema(description = "출발지 경도", nullable = true, example = "127.027610") BigDecimal longitude,
            @Schema(description = "이동 수단", allowableValues = {"PUBLIC_TRANSIT", "CAR"}, example = "PUBLIC_TRANSIT") String transportationMode
    ) {
    }
}
