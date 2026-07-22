package com.moyeo.controller.place;

import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.service.place.SavePlaceCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = """
        내 장소 저장 요청입니다.

        `POST /api/departure-places/searches` 응답의 `results` 중 사용자가 선택한 항목에서
        `type`, `displayName`, `address`, `roadAddress`, `jibunAddress`, `latitude`, `longitude`를
        그대로 복사하고, 사용자가 입력한 `alias`를 추가해 전송합니다.

        서버는 선택 당시의 검색 결과를 회원 소유 스냅샷으로 저장합니다. 같은 검색 결과나
        같은 별칭을 여러 번 저장할 수 있으며 현재 저장 개수 제한은 없습니다.
        """)
public record SavePlaceRequest(
        @Schema(description = "회원이 지정한 장소 별칭", example = "회사", maxLength = 30)
        @NotBlank @Size(max = 30) String alias,

        @Schema(
                description = """
                        출발지 검색 응답의 결과 유형을 그대로 전달합니다.
                        - `STATION`: 지하철역 검색 결과
                        - `ADDRESS`: 도로명주소 또는 지번주소 검색 결과
                        - `PLACE`: 상호·시설 등 일반 장소 검색 결과

                        저장 장소의 원본 분류를 보존하는 값이며, 별칭 수정이나 삭제 권한에는 영향을 주지 않습니다.
                        """,
                allowableValues = {"STATION", "ADDRESS", "PLACE"},
                example = "PLACE"
        )
        @NotNull DeparturePlaceType type,

        @Schema(description = "출발지 검색 응답의 `displayName`을 그대로 전달합니다. 카카오 검색 결과에서 화면에 표시한 원본 장소명입니다.", example = "강남파이낸스센터", maxLength = 255)
        @NotBlank @Size(max = 255) String displayName,

        @Schema(description = "출발지 검색 응답의 `address`를 그대로 전달합니다. 도로명주소가 있으면 도로명주소가 대표 주소로 제공됩니다.", example = "서울 강남구 테헤란로 152", maxLength = 255)
        @NotBlank @Size(max = 255) String address,

        @Schema(description = "출발지 검색 응답의 `roadAddress`를 그대로 전달합니다. 검색 결과에 도로명주소가 없으면 생략하거나 null로 전달합니다.", nullable = true, maxLength = 255)
        @Size(max = 255) String roadAddress,

        @Schema(description = "출발지 검색 응답의 `jibunAddress`를 그대로 전달합니다. 검색 결과에 지번주소가 없으면 생략하거나 null로 전달합니다.", nullable = true, maxLength = 255)
        @Size(max = 255) String jibunAddress,

        @Schema(description = "출발지 검색 응답의 WGS84 `latitude`를 그대로 전달합니다. -90 이상 90 이하이며 소수점 이하는 최대 15자리입니다.", example = "37.500028")
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 3, fraction = 15) BigDecimal latitude,

        @Schema(description = "출발지 검색 응답의 WGS84 `longitude`를 그대로 전달합니다. -180 이상 180 이하이며 소수점 이하는 최대 15자리입니다.", example = "127.036502")
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 15) BigDecimal longitude
) {

    public SavePlaceRequest {
        alias = strip(alias);
    }

    public SavePlaceCommand toCommand() {
        return new SavePlaceCommand(
                alias,
                type,
                displayName,
                address,
                roadAddress,
                jibunAddress,
                latitude,
                longitude
        );
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
