package com.moyeo.controller.meeting;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyeo.service.meeting.MyMeetingListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "로그인 회원의 홈 모임 목록")
public record MyMeetingListResponse(
        @Schema(description = "아직 최종 확정되지 않은 모임 목록") List<Item> planningMeetings,
        @Schema(description = "최종 확정된 모임 목록") List<Item> confirmedMeetings
) {

    public static MyMeetingListResponse from(MyMeetingListResult result) {
        return new MyMeetingListResponse(
                result.planningMeetings().stream().map(Item::from).toList(),
                result.confirmedMeetings().stream().map(Item::from).toList()
        );
    }

    @Schema(description = "홈 모임 카드")
    public record Item(
            @Schema(description = "모임 ID", example = "17") Long meetingId,
            @Schema(description = "모임명", example = "주말 저녁 모임") String name,
            @Schema(description = "커버 이미지 조회 API 경로. 없으면 null") String coverImageUrl,
            @Schema(description = "모임장 닉네임", example = "moyeo1") String hostNickname,
            @Schema(description = "나의 역할", allowableValues = {"HOST", "MEMBER"}) String role,
            @Schema(description = "현재 참여 인원", example = "2") int participantCount,
            @Schema(description = "최대 참여 인원", example = "6") int maxParticipants,
            @Schema(description = "참여·응답 마감 상태", allowableValues = {"OPEN", "CLOSED", "NO_DEADLINE"}) String deadlineStatus,
            @Schema(description = "마감 시각. 마감이 없으면 null", example = "2026-07-29 13:54:43")
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime deadlineAt,
            @Schema(description = "확정 일정 날짜. 미확정 또는 장소 전용 모임이면 null") LocalDate confirmedScheduleDate,
            @Schema(description = "확정 시작 시간. DATE_ONLY 또는 장소 전용 모임이면 null") LocalTime confirmedStartTime,
            @Schema(description = "확정 종료 시간. DATE_ONLY 또는 장소 전용 모임이면 null") LocalTime confirmedEndTime,
            @Schema(description = "확정 장소명. 미확정 또는 일정 전용 모임이면 null") String confirmedPlaceName
    ) {

        static Item from(MyMeetingListResult.Item item) {
            return new Item(
                    item.meetingId(), item.name(), item.coverImageUrl(), item.hostNickname(), item.role(),
                    item.participantCount(), item.maxParticipants(), item.deadlineStatus(), item.deadlineAt(),
                    item.confirmedScheduleDate(), item.confirmedStartTime(), item.confirmedEndTime(), item.confirmedPlaceName()
            );
        }
    }
}
