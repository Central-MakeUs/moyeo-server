package com.moyeo.controller.departure;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "출발지 통합 검색 요청")
public record DeparturePlaceSearchRequest(
        @Schema(description = "역명, 도로명·지번 주소 또는 일반 장소명", example = "서울역")
        @NotBlank @Size(max = 100) String keyword
) {
}
