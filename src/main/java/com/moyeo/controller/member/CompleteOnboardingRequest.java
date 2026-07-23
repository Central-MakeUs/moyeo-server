package com.moyeo.controller.member;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "최초 닉네임 등록 요청")
public record CompleteOnboardingRequest(
        @Schema(description = "사용자 기본 닉네임. 전역 고유값이 아닙니다.", example = "모여", minLength = 1, maxLength = 30)
        @NotBlank
        @Size(max = 30)
        String nickname
) {
}
