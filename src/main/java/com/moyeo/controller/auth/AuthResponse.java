package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 성공 응답")
public record AuthResponse(
        @Schema(description = "API 인증에 사용할 Access Token. 보호된 API 호출 시 Authorization 헤더에 넣습니다.", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "토큰 타입. Authorization 헤더에는 `Bearer {accessToken}` 형태로 사용합니다.", example = "Bearer")
        String tokenType,

        @Schema(description = "회원가입 또는 로그인에 성공한 사용자 정보")
        AuthUserResponse user
) {

    private static final String BEARER = "Bearer";

    public static AuthResponse of(String accessToken, AuthenticatedMember member) {
        return new AuthResponse(accessToken, BEARER, AuthUserResponse.from(member));
    }
}
