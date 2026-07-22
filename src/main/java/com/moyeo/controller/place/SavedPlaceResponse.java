package com.moyeo.controller.place;

import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.service.place.SavedPlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "회원이 저장한 장소 스냅샷입니다. `alias`만 수정할 수 있으며 검색 결과에서 가져온 나머지 위치 정보는 생성 후 변경되지 않습니다.")
public record SavedPlaceResponse(
        @Schema(description = "저장 장소 ID", example = "1") Long id,
        @Schema(description = "회원이 지정한 장소 별칭", example = "회사") String alias,
        @Schema(
                description = "원본 검색 결과 유형입니다. `STATION`은 지하철역, `ADDRESS`는 도로명·지번주소, `PLACE`는 상호·시설 등 일반 장소를 의미합니다.",
                allowableValues = {"STATION", "ADDRESS", "PLACE"},
                example = "PLACE"
        )
        DeparturePlaceType type,
        @Schema(description = "검색 결과의 원본 표시명", example = "강남파이낸스센터") String displayName,
        @Schema(description = "대표 주소", example = "서울 강남구 테헤란로 152") String address,
        @Schema(description = "도로명주소. 없으면 null입니다.", nullable = true) String roadAddress,
        @Schema(description = "지번주소. 없으면 null입니다.", nullable = true) String jibunAddress,
        @Schema(description = "WGS84 위도", example = "37.500028") BigDecimal latitude,
        @Schema(description = "WGS84 경도", example = "127.036502") BigDecimal longitude
) {

    public static SavedPlaceResponse from(SavedPlaceResult result) {
        return new SavedPlaceResponse(
                result.id(),
                result.alias(),
                result.type(),
                result.displayName(),
                result.address(),
                result.roadAddress(),
                result.jibunAddress(),
                result.latitude(),
                result.longitude()
        );
    }
}
