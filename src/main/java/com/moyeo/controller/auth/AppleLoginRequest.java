package com.moyeo.controller.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Apple 로그인 요청")
public record AppleLoginRequest(
        @Schema(description = "Apple이 프론트 콜백에 전달한 일회용 로그인 코드", example = "c123...")
        @NotBlank
        @Size(max = 4096)
        String code,

        @Schema(description = "프론트가 Apple 로그인 요청 전에 생성한 nonce 원문", example = "YpV5...")
        @NotBlank
        @Size(max = 255)
        String nonce
) {
}
