package com.moyeo.controller.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "카카오 네이티브 SDK 로그인 요청")
public record KakaoNativeLoginRequest(
        @Schema(description = "카카오 네이티브 SDK가 발급한 Access Token. 서버는 사용자 식별 후 저장하지 않습니다.", example = "kakao-access-token")
        @NotBlank
        @Size(max = 4096)
        String accessToken
) {
}
