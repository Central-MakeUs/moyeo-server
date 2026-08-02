package com.moyeo.controller.meeting;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "게스트 참여 진입 분기 요청입니다.")
public record GuestEntryRequest(
        @Schema(description = "모임 안에서 사용할 게스트 닉네임", example = "guest", minLength = 2, maxLength = 10,
                pattern = "^[가-힣A-Za-z]{2,10}$")
        @NotBlank
        @Pattern(regexp = "^[가-힣A-Za-z]{2,10}$", message = "게스트 닉네임은 한글 또는 영문 2~10자로 입력해주세요.")
        String nickname,

        @Schema(description = "게스트 참여 비밀번호", example = "1234", minLength = 4, maxLength = 4,
                pattern = "^[0-9]{4}$")
        @NotBlank
        @Size(min = 4, max = 4)
        @Pattern(regexp = "^[0-9]{4}$", message = "게스트 비밀번호는 숫자 4자리로 입력해주세요.")
        String password
) {
}
