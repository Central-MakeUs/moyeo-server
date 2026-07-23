package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 사용자 응답")
public record AuthUserResponse(
        @Schema(description = "서버에서 사용자를 식별하는 ID", example = "1")
        Long id,

        @Schema(description = "사용자 기본 닉네임. 최초 소셜 가입 후 온보딩 전에는 null입니다.", example = "moyeo1", nullable = true)
        String nickname,

        @Schema(description = "닉네임 등록을 완료했는지 여부", example = "true")
        boolean onboardingCompleted
) {

    public static AuthUserResponse from(AuthenticatedMember member) {
        return new AuthUserResponse(member.userId(), member.nickname(), member.onboardingCompleted());
    }
}
