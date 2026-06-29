package com.moyeo.controller.room;

import com.moyeo.domain.room.PlaceMode;
import com.moyeo.domain.room.PlaceRecommendationStrategy;
import com.moyeo.domain.room.ScheduleMode;
import com.moyeo.service.room.CreateRoomCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "모임 생성 요청")
public record CreateRoomRequest(
        @Schema(description = "모임 이름", example = "토요일 저녁 모임", minLength = 1, maxLength = 15)
        @NotBlank
        @Size(min = 1, max = 15)
        String name,

        @Schema(description = "모임 설명", example = "오랜만에 같이 저녁 먹어요.", maxLength = 100)
        @Size(max = 100)
        String description,

        @Schema(description = "최대 참여 인원. 방장 포함 기준입니다.", example = "6", minimum = "2", maximum = "20")
        @Min(2)
        @Max(20)
        int maxParticipants,

        @Schema(description = "일정 설정 방식", example = "VOTE", allowableValues = {"VOTE", "FIXED", "NONE"})
        @NotNull
        ScheduleMode scheduleMode,

        @Schema(description = "이미 확정된 일정. scheduleMode가 FIXED일 때 사용합니다.", example = "2026-07-04T19:00:00")
        LocalDateTime fixedScheduleAt,

        @Schema(description = "일정 조율 후보 날짜 목록. scheduleMode가 VOTE일 때 최대 14개까지 사용합니다.")
        @Size(max = 14)
        List<LocalDate> scheduleCandidateDates,

        @Schema(description = "일정 조율 시작 시간. scheduleMode가 VOTE일 때 사용하며 1시간 단위로 입력합니다.", example = "09:00")
        LocalTime availableStartTime,

        @Schema(description = "일정 조율 종료 시간. scheduleMode가 VOTE일 때 사용하며 1시간 단위로 입력합니다.", example = "18:00")
        LocalTime availableEndTime,

        @Schema(description = "장소 설정 방식", example = "RECOMMEND", allowableValues = {"FIXED", "RECOMMEND", "NONE"})
        @NotNull
        PlaceMode placeMode,

        @Schema(description = "장소 추천 방식. placeMode가 RECOMMEND일 때 사용합니다.", example = "MIDDLE_POINT", allowableValues = {"MIDDLE_POINT", "RANDOM"})
        PlaceRecommendationStrategy placeRecommendationStrategy,

        @Schema(description = "이미 확정된 장소 이름. placeMode가 FIXED일 때 사용합니다.", example = "강남역")
        @Size(max = 100)
        String fixedPlaceName,

        @Schema(description = "이미 확정된 장소 주소. placeMode가 FIXED일 때 사용합니다.", example = "서울 강남구 강남대로 지하 396")
        @Size(max = 255)
        String fixedPlaceAddress,

        @Schema(description = "생성 시점 기준 마감까지 남은 시간(분). 서버가 deadlineAt으로 계산합니다. 10분 단위로 입력합니다.", example = "1440", minimum = "10", maximum = "4320")
        @Min(10)
        @Max(4320)
        int deadlineMinutes
) {

    @AssertTrue(message = "일정 투표는 후보 날짜와 시간대가 필요합니다.")
    @Schema(hidden = true)
    public boolean isValidVoteSchedule() {
        if (scheduleMode != ScheduleMode.VOTE) {
            return true;
        }
        return scheduleCandidateDates != null
                && !scheduleCandidateDates.isEmpty()
                && availableStartTime != null
                && availableEndTime != null
                && availableStartTime.isBefore(availableEndTime)
                && isHourUnit(availableStartTime)
                && isHourUnit(availableEndTime);
    }

    @AssertTrue(message = "확정 일정은 fixedScheduleAt이 필요합니다.")
    @Schema(hidden = true)
    public boolean isValidFixedSchedule() {
        return scheduleMode != ScheduleMode.FIXED || fixedScheduleAt != null;
    }

    @AssertTrue(message = "장소 추천은 추천 방식이 필요합니다.")
    @Schema(hidden = true)
    public boolean isValidPlaceRecommendation() {
        return placeMode != PlaceMode.RECOMMEND || placeRecommendationStrategy != null;
    }

    @AssertTrue(message = "확정 장소는 장소 이름과 주소가 필요합니다.")
    @Schema(hidden = true)
    public boolean isValidFixedPlace() {
        return placeMode != PlaceMode.FIXED || (hasText(fixedPlaceName) && hasText(fixedPlaceAddress));
    }

    @AssertTrue(message = "마감 시간은 10분 단위로 입력해야 합니다.")
    @Schema(hidden = true)
    public boolean isValidDeadlineUnit() {
        return deadlineMinutes % 10 == 0;
    }

    public CreateRoomCommand toCommand() {
        return new CreateRoomCommand(
                name,
                description,
                maxParticipants,
                scheduleMode,
                fixedScheduleAt,
                scheduleCandidateDates != null ? scheduleCandidateDates : List.of(),
                availableStartTime,
                availableEndTime,
                placeMode,
                placeRecommendationStrategy,
                fixedPlaceName,
                fixedPlaceAddress,
                deadlineMinutes
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isHourUnit(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }
}
