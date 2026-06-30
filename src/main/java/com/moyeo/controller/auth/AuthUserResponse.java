package com.moyeo.controller.auth;

import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증 사용자 응답")
public record AuthUserResponse(
        @Schema(description = "서버에서 사용자를 식별하는 ID", example = "1")
        Long id,

        @Schema(description = "사용자 기본 닉네임. 모임 안 표시 닉네임과는 별도로 관리될 수 있습니다.", example = "moyeo1")
        String nickname
) {

    public static AuthUserResponse from(AuthenticatedMember member) {
        return new AuthUserResponse(member.userId(), member.nickname());
    }
}
