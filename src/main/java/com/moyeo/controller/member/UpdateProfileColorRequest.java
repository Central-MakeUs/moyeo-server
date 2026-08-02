package com.moyeo.controller.member;

import com.moyeo.domain.member.ProfileColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateProfileColorRequest(
        @NotNull
        @Schema(description = "선택할 기본 프로필 색상", example = "PURPLE", allowableValues = {"GRAY", "RED", "PURPLE", "ORANGE"})
        ProfileColor color
) {
}
