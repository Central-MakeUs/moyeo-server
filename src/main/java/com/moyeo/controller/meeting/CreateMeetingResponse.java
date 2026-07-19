package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MeetingCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 생성 성공 응답입니다. 방장 참여 화면 진입에 필요한 모임 ID만 반환합니다.")
public record CreateMeetingResponse(
        @Schema(description = "생성된 모임 식별자입니다.", example = "1")
        Long meetingId
) {

    public static CreateMeetingResponse from(MeetingCreateResult result) {
        return new CreateMeetingResponse(result.meetingId());
    }
}
