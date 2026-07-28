package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ConfirmPlaceCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "장소 확정 요청")
public record ConfirmPlaceRequest(@Schema(description = "장소 추천 또는 실제 이동시간 추천 응답의 areaCode", example = "1001491", requiredMode = Schema.RequiredMode.REQUIRED) String commercialAreaCode) {
    public ConfirmPlaceCommand toCommand() { return new ConfirmPlaceCommand(commercialAreaCode); }
}
