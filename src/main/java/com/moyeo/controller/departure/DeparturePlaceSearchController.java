package com.moyeo.controller.departure;

import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.departure.DeparturePlaceSearchApplicationService;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.meeting.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/departure-places")
@Tag(name = "Departure place", description = "출발지 통합 검색 API")
@Validated
public class DeparturePlaceSearchController {

    private final DeparturePlaceSearchApplicationService departurePlaceSearchService;
    private final MeetingService meetingService;

    public DeparturePlaceSearchController(
            DeparturePlaceSearchApplicationService departurePlaceSearchService,
            MeetingService meetingService
    ) {
        this.departurePlaceSearchService = departurePlaceSearchService;
        this.meetingService = meetingService;
    }

    @PostMapping("/searches")
    @Operation(
            summary = "출발지 통합 검색",
            description = """
                    인증:
                    - Access Token: 선택 — 회원 검색 시 사용
                    - inviteCode: 조건부 필수 — Access Token이 없는 게스트에게 필수
                    - 둘 다 전달: Access Token 우선, inviteCode 무시
                    - 잘못된 Access Token: 401, inviteCode 방식으로 fallback하지 않음

                    통합 이유:
                    - 회원과 게스트의 검색 요청·응답 및 카카오 검색 로직이 동일함
                    - 인증 수단만 서버에서 분기해 하나의 API로 제공함

                    검색:
                    - 정확히 `~역`: 지하철역 우선 검색 후 결과가 없으면 일반 장소 검색
                    - 도로명·지번 주소형(`번지` 포함): 주소 우선 검색 후 결과가 없으면 일반 장소 검색
                    - 단독 지명·지하 주소 및 그 외: 일반 장소 검색
                    - 결과 좌표: WGS84 위도·경도 포함
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "STATION", value = """
                            { "keyword": "서울역" }
                            """),
                    @ExampleObject(name = "ADDRESS", value = """
                            { "keyword": "서울 강남구 테헤란로 152" }
                            """),
                    @ExampleObject(name = "PLACE", value = """
                            { "keyword": "스타벅스 강남역점" }
                            """)
            }))
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "출발지 통합 검색 성공"),
            @ApiResponse(responseCode = "400", description = "검색어 검증 실패", content = @Content(examples = @ExampleObject(value = """
                    { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                    """))),
            @ApiResponse(responseCode = "401", description = "Access Token과 초대코드가 모두 없거나, 전달한 Access Token이 유효하지 않음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """))),
            @ApiResponse(responseCode = "404", description = "게스트 요청의 초대코드에 해당하는 모임 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "MEETING_INVITATION_NOT_FOUND", "status": 404 }
                    """))),
            @ApiResponse(responseCode = "503", description = "출발지 검색 서비스 또는 카카오 REST API 키를 사용할 수 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "DEPARTURE_PLACE_SEARCH_UNAVAILABLE", "status": 503 }
                    """)))
    })
    public DeparturePlaceSearchResponse search(
            @Parameter(hidden = true) @CurrentMember(required = false) AuthenticatedMember member,
            @Parameter(description = "회원: 선택(전달해도 무시) / 게스트: 필수인 모임 초대코드", example = "ABCD234567")
            @RequestParam(required = false) String inviteCode,
            @Valid @RequestBody DeparturePlaceSearchRequest request
    ) {
        if (member != null) {
            return DeparturePlaceSearchResponse.from(
                    departurePlaceSearchService.search(request.keyword())
            );
        }
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED);
        }
        meetingService.validateInvitationExists(inviteCode.strip());
        return DeparturePlaceSearchResponse.from(
                departurePlaceSearchService.search(request.keyword())
        );
    }

    @GetMapping("/reverse-geocodes")
    @Operation(
            summary = "좌표로 출발지 주소 조회",
            description = """
                    WGS84 위도와 경도를 카카오 Local로 변환해 도로명주소와 지번주소를 반환합니다.

                    인증은 출발지 통합 검색과 같습니다.
                    - Access Token을 보낸 회원은 inviteCode 없이 요청할 수 있습니다.
                    - Access Token이 없으면 유효한 모임 inviteCode가 필요합니다.
                    - 도로명주소는 카카오 결과에 없을 수 있으며, 이때 null입니다.
                    - 주소를 찾지 못하면 두 주소 필드가 모두 null인 200 응답을 반환합니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주소 변환 성공 또는 해당 좌표의 주소 없음"),
            @ApiResponse(responseCode = "400", description = "좌표 검증 실패", content = @Content(examples = @ExampleObject(value = """
                    { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                    """))),
            @ApiResponse(responseCode = "401", description = "Access Token과 inviteCode가 모두 없거나 Access Token이 유효하지 않음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """))),
            @ApiResponse(responseCode = "404", description = "게스트 요청의 inviteCode에 해당하는 모임 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "MEETING_INVITATION_NOT_FOUND", "status": 404 }
                    """))),
            @ApiResponse(responseCode = "503", description = "카카오 Local API를 사용할 수 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "REVERSE_GEOCODING_UNAVAILABLE", "status": 503 }
                    """)))
    })
    public ReverseGeocodingResponse reverseGeocode(
            @Parameter(hidden = true) @CurrentMember(required = false) AuthenticatedMember member,
            @Parameter(description = "회원은 선택이며 게스트는 유효한 모임 초대코드가 필요합니다.", example = "ABCD234567")
            @RequestParam(required = false) String inviteCode,
            @Parameter(description = "WGS84 위도", example = "37.5665")
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") java.math.BigDecimal latitude,
            @Parameter(description = "WGS84 경도", example = "126.9780")
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") java.math.BigDecimal longitude
    ) {
        if (member == null) {
            if (inviteCode == null || inviteCode.isBlank()) {
                throw new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED);
            }
            meetingService.validateInvitationExists(inviteCode.strip());
        }
        return ReverseGeocodingResponse.from(departurePlaceSearchService.reverseGeocode(latitude, longitude));
    }
}
