package com.moyeo.controller.room;

import com.moyeo.service.room.RoomCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "모임 생성 응답")
public record CreateRoomResponse(
        @Schema(description = "모임 ID", example = "1")
        Long roomId,

        @Schema(description = "모임 이름", example = "토요일 저녁 모임")
        String name,

        @Schema(description = "모임 설명", example = "오랜만에 같이 저녁 먹어요.")
        String description,

        @Schema(description = "최대 참여 인원. 방장 포함 기준입니다.", example = "6")
        Integer maxParticipants,

        @Schema(description = "일정 설정 방식", example = "VOTE")
        String scheduleMode,

        @Schema(description = "확정 일정", example = "2026-07-04T19:00:00")
        LocalDateTime fixedScheduleAt,

        @Schema(description = "일정 후보 날짜 목록")
        List<LocalDate> scheduleCandidateDates,

        @Schema(description = "일정 조율 시작 시간", example = "09:00")
        LocalTime availableStartTime,

        @Schema(description = "일정 조율 종료 시간", example = "18:00")
        LocalTime availableEndTime,

        @Schema(description = "장소 설정 방식", example = "RECOMMEND")
        String placeMode,

        @Schema(description = "장소 추천 방식", example = "MIDDLE_POINT")
        String placeRecommendationStrategy,

        @Schema(description = "확정 장소 이름", example = "강남역")
        String fixedPlaceName,

        @Schema(description = "확정 장소 주소", example = "서울 강남구 강남대로 지하 396")
        String fixedPlaceAddress,

        @Schema(description = "서버가 계산한 모임 응답 마감 일시", example = "2026-07-01T18:00:00")
        LocalDateTime deadlineAt,

        @Schema(description = "초대 코드", example = "ABCD234567")
        String inviteCode,

        @Schema(description = "프론트에서 초대 링크를 만들 때 사용할 수 있는 경로", example = "/rooms/invitations/ABCD234567")
        String invitePath,

        @Schema(description = "방장 참여자 ID", example = "1")
        Long hostParticipantId
) {

    public static CreateRoomResponse from(RoomCreateResult result) {
        return new CreateRoomResponse(
                result.roomId(),
                result.name(),
                result.description(),
                result.maxParticipants(),
                result.scheduleMode(),
                result.fixedScheduleAt(),
                result.scheduleCandidateDates(),
                result.availableStartTime(),
                result.availableEndTime(),
                result.placeMode(),
                result.placeRecommendationStrategy(),
                result.fixedPlaceName(),
                result.fixedPlaceAddress(),
                result.deadlineAt(),
                result.inviteCode(),
                result.invitePath(),
                result.hostParticipantId()
        );
    }
}
