package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.SaveParticipationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "방장 참여 완료 요청입니다. 일정 조율 모임에서는 방장만 후보 날짜를 확정합니다.")
public record HostParticipationRequest(
        @Schema(description = "방장이 선택한 모임 일정 후보 날짜입니다. 일정 조율 모임에서 필수입니다.")
        List<@NotNull LocalDate> scheduleCandidateDates,

        @Schema(description = "방장의 일정 응답입니다. 날짜만 조율할 때는 생략하고 후보 날짜를 방장의 가능 날짜로 저장합니다.")
        @Valid SaveParticipationRequest.ScheduleResponseRequest scheduleResponse,

        @Schema(description = "방장의 출발지와 이동수단입니다. 장소 조율 모임에서 필수입니다.")
        @Valid SaveParticipationRequest.DepartureRequest departure
) {

    public SaveParticipationCommand toParticipationCommand() {
        return SaveParticipationRequest.toCommand(scheduleResponse, departure);
    }
}
