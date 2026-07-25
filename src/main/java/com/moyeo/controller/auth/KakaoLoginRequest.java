package com.moyeo.controller.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "카카오 로그인 요청")
public record KakaoLoginRequest(
        @Schema(description = "카카오 콜백으로 전달된 일회용 인가 코드", example = "abc123...")
        @NotBlank
        @Size(max = 4096)
        String code
) {
}
