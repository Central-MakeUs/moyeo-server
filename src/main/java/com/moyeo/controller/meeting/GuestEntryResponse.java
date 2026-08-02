package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.GuestEntryResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게스트 참여 진입 분기 응답")
public record GuestEntryResponse(
        @Schema(description = "NEW_GUEST는 신규 참여 입력으로, EXISTING_GUEST는 모임 현황으로 이동합니다.",
                allowableValues = {"NEW_GUEST", "EXISTING_GUEST"})
        String entryType
) {
    static GuestEntryResponse from(GuestEntryResult result) {
        return new GuestEntryResponse(result.entryType());
    }
}
