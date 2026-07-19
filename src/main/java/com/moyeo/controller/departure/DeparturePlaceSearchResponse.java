package com.moyeo.controller.departure;

import com.moyeo.departure.DeparturePlaceSearchService;
import com.moyeo.departure.DeparturePlaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "출발지 통합 검색 응답. 선택 후보의 WGS84 위도와 경도를 포함합니다.")
public record DeparturePlaceSearchResponse(
        @Schema(description = "검색 결과 목록") List<Result> results
) {

    public static DeparturePlaceSearchResponse from(DeparturePlaceSearchService.DeparturePlaceSearchResult result) {
        return new DeparturePlaceSearchResponse(result.places().stream().map(Result::from).toList());
    }

    @Schema(description = "출발지 선택 후보")
    public record Result(
            @Schema(description = "검색 결과 유형", allowableValues = {"STATION", "ADDRESS", "PLACE"}, example = "STATION")
            DeparturePlaceType type,
            @Schema(description = "검색 목록에 표시할 장소명 또는 주소 표시명입니다. 최종 출발지 name은 사용자가 별도로 입력합니다.", example = "서울역 1호선")
            String displayName,
            @Schema(description = "선택 시 출발지 주소로 사용할 대표 주소입니다. 도로명주소를 우선합니다.", example = "서울 중구 세종대로 지하 2")
            String address,
            @Schema(description = "도로명주소. 없으면 null입니다.", nullable = true)
            String roadAddress,
            @Schema(description = "지번주소. 없으면 null입니다.", nullable = true)
            String jibunAddress,
            @Schema(description = "선택 후보의 WGS84 위도입니다.", example = "37.5546788388674")
            BigDecimal latitude,
            @Schema(description = "선택 후보의 WGS84 경도입니다.", example = "126.970606917394")
            BigDecimal longitude
    ) {
        static Result from(DeparturePlaceSearchService.DeparturePlaceSearchResult.Place place) {
            return new Result(
                    place.type(),
                    place.displayName(),
                    place.address(),
                    place.roadAddress(),
                    place.jibunAddress(),
                    place.latitude(),
                    place.longitude()
            );
        }
    }
}
