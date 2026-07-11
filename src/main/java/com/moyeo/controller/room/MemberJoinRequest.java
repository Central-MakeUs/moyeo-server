package com.moyeo.controller.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 회원 모임 참여 요청")
public record MemberJoinRequest(
        @Schema(
                description = "모임 안에서 사용할 표시 닉네임입니다. 회원 기본 닉네임과 다르게 입력할 수 있습니다.",
                example = "member1",
                minLength = 1,
                maxLength = 30
        )
        @NotBlank
        @Size(min = 1, max = 30)
        String nickname,

        @Schema(
                description = "참여 비밀번호입니다. 추후 참여자 재입장/수정 검증 흐름에서 사용할 수 있도록 해시로 저장합니다.",
                example = "memberpass123",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
