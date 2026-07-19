package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.HostParticipationResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "방장 참여 완료 응답입니다. 링크 공유에 필요한 초대 정보를 반환합니다.")
public record HostParticipationResponse(
        @Schema(description = "참여를 완료한 모임 식별자입니다.", example = "1")
        Long meetingId,

        @Schema(description = "초대 코드입니다.", example = "ABCD234567")
        String inviteCode,

        @Schema(description = "프론트가 공유 링크를 만들 때 사용하는 상대 경로입니다.", example = "/meetings/invitations/ABCD234567")
        String invitePath
) {

    public static HostParticipationResponse from(HostParticipationResult result) {
        return new HostParticipationResponse(result.meetingId(), result.inviteCode(), result.invitePath());
    }
}
