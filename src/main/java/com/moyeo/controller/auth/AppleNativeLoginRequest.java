package com.moyeo.controller.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Apple 네이티브 SDK 로그인 요청")
public record AppleNativeLoginRequest(
        @Schema(description = "Apple 네이티브 SDK가 발급한 identity token", example = "eyJraWQiOi...")
        @NotBlank
        @Size(max = 8192)
        String identityToken,

        @Schema(description = "Apple 네이티브 SDK가 발급한 일회용 authorization code", example = "c123...")
        @NotBlank
        @Size(max = 4096)
        String authorizationCode,

        @Schema(description = "프론트가 native Apple 로그인 요청 전에 생성해 SDK에 전달한 nonce 원문", example = "YpV5...")
        @NotBlank
        @Size(max = 255)
        String nonce
) {
}
