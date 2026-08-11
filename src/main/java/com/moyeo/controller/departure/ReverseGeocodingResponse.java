package com.moyeo.controller.departure;

import com.moyeo.departure.DeparturePlaceSearchService;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "좌표에 해당하는 주소 정보입니다. 카카오가 해당 주소를 제공하지 않으면 각 필드는 null입니다.")
public record ReverseGeocodingResponse(
        @Schema(description = "도로명주소. 해당 좌표에 도로명주소가 없으면 null입니다.", nullable = true,
                example = "서울 중구 세종대로 110")
        String roadAddress,
        @Schema(description = "지번주소. 해당 좌표에 지번주소가 없으면 null입니다.", nullable = true,
                example = "서울 중구 태평로1가 31")
        String jibunAddress
) {

    static ReverseGeocodingResponse from(DeparturePlaceSearchService.ReverseGeocodingResult result) {
        return new ReverseGeocodingResponse(result.roadAddress(), result.jibunAddress());
    }
}
