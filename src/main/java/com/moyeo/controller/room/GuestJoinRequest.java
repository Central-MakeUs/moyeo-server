package com.moyeo.controller.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = """
        게스트 모임 참여 요청입니다.
        현재 게스트 참여 API는 참여자 생성에 필요한 닉네임과 비밀번호만 받습니다.
        출발지 주소, 좌표, 이동수단 입력은 1차 일정 참여 플로우 이후 장소 조율 참여 API에서 별도로 다룹니다.
        """)
public record GuestJoinRequest(
        @Schema(
                description = "모임 안에서 사용할 표시 닉네임입니다. 같은 모임 안에서는 중복될 수 없습니다.",
                example = "guest1",
                minLength = 1,
                maxLength = 30
        )
        @NotBlank
        @Size(min = 1, max = 30)
        String nickname,

        @Schema(
                description = """
                        게스트 참여 비밀번호입니다.
                        현재는 참여 정보에 해시로 저장하며, 게스트 재입장/수정 검증 정책은 아직 구현하지 않았습니다.
                        """,
                example = "guestpass123",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
