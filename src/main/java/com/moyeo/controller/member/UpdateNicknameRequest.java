package com.moyeo.controller.member;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "기본 닉네임 수정 요청")
public record UpdateNicknameRequest(
        @Schema(
                description = "사용자 기본 닉네임입니다. 한글 또는 영문 2~10자이며, 모임 안에서 사용하는 참여자 닉네임은 변경하지 않습니다.",
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
