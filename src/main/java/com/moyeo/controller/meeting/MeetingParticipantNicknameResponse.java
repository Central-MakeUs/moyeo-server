package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingParticipantNicknameResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 내 닉네임 수정 응답")
public record MeetingParticipantNicknameResponse(
        @Schema(description = "모임 ID", example = "1") Long meetingId,
        @Schema(description = "수정된 참여자 ID", example = "10") Long participantId,
        @Schema(description = "수정된 모임 내 닉네임", example = "모여") String nickname
) {

    public static MeetingParticipantNicknameResponse from(MeetingParticipantNicknameResult result) {
        return new MeetingParticipantNicknameResponse(result.meetingId(), result.participantId(), result.nickname());
    }
}
