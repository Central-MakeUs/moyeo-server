package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.SaveParticipationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = """
        게스트 모임 참여 요청입니다.
        게스트 참여자 생성과 참여 상세 정보 저장을 한 번에 처리합니다.
        일정 조율 모임은 scheduleResponse를, 장소 조율 모임은 departure를 함께 입력해야 합니다.
        """)
public record GuestJoinRequest(
        @Schema(
                description = "모임 안에서 사용할 표시 닉네임입니다. 같은 모임 안에서는 중복될 수 없습니다.",
                example = "guest",
                minLength = 2,
                maxLength = 10,
                pattern = "^[가-힣A-Za-z]{2,10}$"
        )
        @NotBlank
        @Pattern(regexp = "^[가-힣A-Za-z]{2,10}$", message = "게스트 닉네임은 한글 또는 영문 2~10자로 입력해주세요.")
        String nickname,

        @Schema(
                description = """
                        게스트 참여 비밀번호입니다.
                        현재는 참여 정보에 해시로 저장하며, 게스트 재입장/수정 검증 정책은 아직 구현하지 않았습니다.
                        """,
                example = "1234",
                minLength = 4,
                maxLength = 4,
                pattern = "^[0-9]{4}$"
        )
        @NotBlank
        @Size(min = 4, max = 4)
        @Pattern(regexp = "^[0-9]{4}$", message = "게스트 비밀번호는 숫자 4자리로 입력해주세요.")
        String password,

        @Schema(description = "참여자의 일정 응답입니다. 일정 조율 모임에서 필수입니다.")
        @Valid SaveParticipationRequest.ScheduleResponseRequest scheduleResponse,

        @Schema(description = "참여자 출발지와 이동수단입니다. 장소 조율 모임에서 필수입니다.")
        @Valid
        SaveParticipationRequest.DepartureRequest departure
) {

    public SaveParticipationCommand toParticipationCommand() {
        return SaveParticipationRequest.toCommand(scheduleResponse, departure);
    }
}
