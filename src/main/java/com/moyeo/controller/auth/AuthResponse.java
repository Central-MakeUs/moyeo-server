package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 성공 응답")
public record AuthResponse(
        @Schema(description = "API 인증에 사용할 Access Token. 보호된 API 호출 시 Authorization 헤더에 넣습니다.", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "30일 자동 로그인에 사용할 갱신 토큰입니다. 클라이언트의 안전한 저장소에만 보관하고, 갱신 또는 로그아웃 요청에만 전송합니다.", example = "mGk7bM0s0O6XqzR4a2GfRk6vJ3YwQ2KpT8xD1cL9eHs")
        String refreshToken,

        @Schema(description = "토큰 타입. Authorization 헤더에는 `Bearer {accessToken}` 형태로 사용합니다.", example = "Bearer")
        String tokenType,

        @Schema(description = "회원가입 또는 로그인에 성공한 사용자 정보")
        AuthUserResponse user
) {

    private static final String BEARER = "Bearer";

    public static AuthResponse of(String accessToken, String refreshToken, AuthenticatedMember member) {
        return new AuthResponse(accessToken, refreshToken, BEARER, AuthUserResponse.from(member));
    }
}
