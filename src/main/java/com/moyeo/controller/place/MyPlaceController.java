package com.moyeo.controller.place;

import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.place.SavedPlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/places")
@Tag(name = "My place", description = "마이페이지 내 장소 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class MyPlaceController {

    private final SavedPlaceService savedPlaceService;

    public MyPlaceController(SavedPlaceService savedPlaceService) {
        this.savedPlaceService = savedPlaceService;
    }

    @GetMapping
    @Operation(
            summary = "내 장소 목록 조회",
            description = """
                    현재 로그인한 회원이 저장한 장소만 조회합니다.

                    - 정렬: `createdAt` 내림차순이며, 저장 시각이 같으면 `id` 내림차순
                    - 중복: 같은 위치나 같은 별칭이 여러 번 저장되어 있으면 각각 반환
                    - 개수: 현재 저장 및 조회 개수 제한 없음
                    - 빈 목록: 오류가 아니라 `{ "places": [] }` 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 장소 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "Access Token 없음, 만료 또는 유효하지 않음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """)))
    })
    public SavedPlaceListResponse findAll(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member
    ) {
        return SavedPlaceListResponse.from(savedPlaceService.findAll(member.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "내 장소 저장",
            description = """
                    출발지 통합 검색에서 사용자가 선택한 결과를 내 장소로 저장합니다.

                    **요청 구성 방법**
                    1. `POST /api/departure-places/searches`로 장소를 검색합니다.
                    2. 응답의 `results`에서 사용자가 하나를 선택합니다.
                    3. 선택 결과의 필드를 그대로 복사하고 사용자가 입력한 `alias`를 추가합니다.

                    `type`은 원본 검색 결과가 지하철역(`STATION`), 주소(`ADDRESS`), 일반 장소(`PLACE`) 중
                    무엇인지 보존합니다. 저장·수정·삭제 권한에는 영향을 주지 않습니다.

                    동일한 위치, 주소, 표시명 또는 별칭의 중복 저장을 허용하며 현재 저장 개수 제한은 없습니다.
                    저장된 위치 스냅샷은 변경하지 않고, 이후 수정 API에서는 별칭만 변경할 수 있습니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
                    {
                      "alias": "회사",
                      "type": "PLACE",
                      "displayName": "강남파이낸스센터",
                      "address": "서울 강남구 테헤란로 152",
                      "roadAddress": "서울 강남구 테헤란로 152",
                      "jibunAddress": "서울 강남구 역삼동 737",
                      "latitude": 37.500028,
                      "longitude": 127.036502
                    }
                    """)))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "내 장소 저장 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(examples = @ExampleObject(value = """
                    { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                    """))),
            @ApiResponse(responseCode = "401", description = "Access Token 없음, 만료 또는 유효하지 않음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """)))
    })
    public SavedPlaceResponse save(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody SavePlaceRequest request
    ) {
        return SavedPlaceResponse.from(savedPlaceService.save(member.userId(), request.toCommand()));
    }

    @PatchMapping("/{savedPlaceId}")
    @Operation(
            summary = "내 장소 별칭 수정",
            description = """
                    현재 로그인한 회원이 소유한 저장 장소의 `alias`만 수정합니다.

                    `type`, `displayName`, 주소, 위도·경도 등 선택 당시의 위치 스냅샷은 변경되지 않습니다.
                    위치 자체를 바꾸려면 기존 장소를 삭제하고 새로운 검색 결과를 저장해야 합니다.
                    다른 회원의 장소 ID도 존재 여부를 노출하지 않고 `SAVED_PLACE_NOT_FOUND`로 응답합니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = """
                    { "alias": "새 회사" }
                    """)))
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 장소 별칭 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청값 검증 실패", content = @Content(examples = @ExampleObject(value = """
                    { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                    """))),
            @ApiResponse(responseCode = "401", description = "Access Token 없음, 만료 또는 유효하지 않음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """))),
            @ApiResponse(responseCode = "404", description = "본인 소유의 저장 장소가 아님 또는 장소 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "SAVED_PLACE_NOT_FOUND", "status": 404 }
                    """)))
    })
    public SavedPlaceResponse rename(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @PathVariable Long savedPlaceId,
            @Valid @RequestBody RenameSavedPlaceRequest request
    ) {
        return SavedPlaceResponse.from(savedPlaceService.rename(member.userId(), savedPlaceId, request.alias()));
    }

    @DeleteMapping("/{savedPlaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "내 장소 삭제",
            description = """
                    현재 로그인한 회원이 소유한 저장 장소를 삭제하고 본문 없이 `204 No Content`를 반환합니다.
                    검색 이력이나 모임 참여자에 이미 저장된 출발지 스냅샷은 함께 삭제되지 않습니다.
                    다른 회원의 장소 ID도 존재 여부를 노출하지 않고 `SAVED_PLACE_NOT_FOUND`로 응답합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "내 장소 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "Access Token 없음, 만료 또는 유효하지 않음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """))),
            @ApiResponse(responseCode = "404", description = "본인 소유의 저장 장소가 아님 또는 장소 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "SAVED_PLACE_NOT_FOUND", "status": 404 }
                    """)))
    })
    public void delete(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @PathVariable Long savedPlaceId
    ) {
        savedPlaceService.delete(member.userId(), savedPlaceId);
    }
}
