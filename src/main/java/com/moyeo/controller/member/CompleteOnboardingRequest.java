package com.moyeo.controller.member;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "최초 닉네임 등록 요청")
public record CompleteOnboardingRequest(
        @Schema(
                description = "사용자 기본 닉네임. 한글 또는 영문 2~10자이며 전역 고유값이 아닙니다.",
                example = "모여",
                minLength = 2,
                maxLength = 10,
                pattern = "^[가-힣A-Za-z]{2,10}$"
        )
        @NotBlank
        @Pattern(
                regexp = "^[가-힣A-Za-z]{2,10}$",
                message = "닉네임은 한글 또는 영문 2~10자로 입력해주세요."
        )
        String nickname
) {
}
