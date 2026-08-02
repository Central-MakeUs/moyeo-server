package com.moyeo.controller.auth;

import com.moyeo.domain.member.ProfileColor;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 프로필 표시 정보입니다. 현재는 기본 색상만 지원합니다.")
public record ProfileResponse(
        @Schema(description = "프로필 종류", example = "COLOR", allowableValues = {"COLOR"}) String type,
        @Schema(description = "기본 프로필 색상", example = "PURPLE", allowableValues = {"GRAY", "RED", "PURPLE", "ORANGE"}) ProfileColor color
) {
    public static ProfileResponse color(ProfileColor color) {
        return new ProfileResponse("COLOR", color);
    }
}
