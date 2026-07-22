package com.moyeo.controller.place;

import com.moyeo.service.place.SavedPlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 장소 목록 응답입니다. 현재 저장 개수 제한은 없으며, `places`는 저장 일시가 최신인 항목부터 정렬됩니다.")
public record SavedPlaceListResponse(
        @Schema(description = "최근 저장순 장소 목록") List<SavedPlaceResponse> places
) {

    public static SavedPlaceListResponse from(List<SavedPlaceResult> results) {
        return new SavedPlaceListResponse(results.stream().map(SavedPlaceResponse::from).toList());
    }
}
