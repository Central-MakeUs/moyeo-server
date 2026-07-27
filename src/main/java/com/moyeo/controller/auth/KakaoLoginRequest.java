package com.moyeo.controller.auth;

import com.moyeo.auth.OAuthRedirectTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "카카오 로그인 요청")
public record KakaoLoginRequest(
        @Schema(description = "카카오 콜백으로 전달된 일회용 인가 코드", example = "abc123...")
        @NotBlank
        @Size(max = 4096)
        String code,

        @Schema(description = "서버에 등록된 카카오 콜백 환경", allowableValues = {"local", "dev", "prod"}, example = "dev")
        @NotNull
        OAuthRedirectTarget redirectTarget
) {
}
