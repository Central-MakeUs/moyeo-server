package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MyMeetingDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "로그인 회원의 모임 상세")
public record MyMeetingDetailResponse(
        @Schema(description = "모임 ID", example = "17") Long meetingId,
        @Schema(description = "모임명", example = "주말 저녁 모임") String name,
        @Schema(description = "모임 설명. 입력하지 않은 경우 null입니다.", nullable = true, types = {"string", "null"}) String description,
        @Schema(description = "커버 이미지 조회 API 경로. 없으면 null입니다.", nullable = true, types = {"string", "null"}) String coverImageUrl,
        @Schema(description = "모임장 닉네임", example = "moyeo1") String hostNickname,
        @Schema(description = "확정 일정 날짜. 장소 전용 모임이면 null입니다.", nullable = true, types = {"string", "null"}) LocalDate confirmedScheduleDate,
        @Schema(description = "확정 시작 시간. DATE_ONLY 또는 장소 전용 모임이면 null입니다.", nullable = true, types = {"string", "null"}) LocalTime confirmedStartTime,
        @Schema(description = "확정 종료 시간. DATE_ONLY 또는 장소 전용 모임이면 null입니다.", nullable = true, types = {"string", "null"}) LocalTime confirmedEndTime,
        @Schema(description = "확정 장소명. 일정 전용 또는 미확정이면 null입니다.", nullable = true, types = {"string", "null"}) String confirmedPlaceName,
        @Schema(description = "참여자 목록") List<Participant> participants
) {

    static MyMeetingDetailResponse from(MyMeetingDetailResult result) {
        return new MyMeetingDetailResponse(
                result.meetingId(), result.name(), result.description(), result.coverImageUrl(), result.hostNickname(),
                result.confirmedScheduleDate(), result.confirmedStartTime(), result.confirmedEndTime(), result.confirmedPlaceName(),
                result.participants().stream().map(Participant::from).toList()
        );
    }

    @Schema(description = "모임 참여자")
    public record Participant(
            @Schema(description = "참여자 ID", example = "31") Long participantId,
            @Schema(description = "연결된 서비스 사용자 ID입니다. 게스트 참여자는 null입니다.", example = "42", nullable = true) Long userId,
            @Schema(description = "모임 안에서 사용하는 닉네임", example = "moyeo1") String nickname,
            @Schema(description = "참여자 유형", allowableValues = {"HOST", "MEMBER", "GUEST"}) String participantType,
            @Schema(description = "현재 로그인한 사용자 여부", example = "true") boolean isMe
    ) {

        private static Participant from(MyMeetingDetailResult.Participant participant) {
            return new Participant(participant.participantId(), participant.userId(), participant.nickname(), participant.participantType(), participant.isMe());
        }
    }
}
