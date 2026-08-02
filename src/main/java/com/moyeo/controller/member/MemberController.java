package com.moyeo.controller.member;

import com.moyeo.controller.auth.AuthUserResponse;
import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberOnboardingService;
import com.moyeo.service.member.MemberWithdrawalService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Member", description = "현재 사용자 정보 API")
public class MemberController {

    private final MemberOnboardingService memberOnboardingService;
    private final MemberWithdrawalService memberWithdrawalService;
    private final SavedPlaceService savedPlaceService;

    public MemberController(
            MemberOnboardingService memberOnboardingService,
            MemberWithdrawalService memberWithdrawalService,
            SavedPlaceService savedPlaceService
    ) {
        this.memberOnboardingService = memberOnboardingService;
        this.memberWithdrawalService = memberWithdrawalService;
        this.savedPlaceService = savedPlaceService;
    }

    @GetMapping
    @Operation(
            summary = "마이페이지 조회",
            description = "현재 회원의 기본 닉네임, 프로필 색상, 저장 출발지 목록을 함께 반환합니다. 출발지 목록은 GET /api/me/places와 같은 데이터를 마이페이지 화면용으로 복제해 반환하며, 피드백 이력은 포함하지 않습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(
                    responseCode = "403",
                    description = "최초 닉네임 온보딩이 완료되지 않은 사용자입니다.",
                    content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = """
                            { "code": "ONBOARDING_REQUIRED", "status": 403 }
                            """))
            )
    })
    public MyPageResponse getMyPage(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member
    ) {
        return MyPageResponse.from(member, savedPlaceService.findAll(member.userId()));
    }

    @PutMapping("/onboarding")
    @Operation(
            summary = "최초 닉네임 등록",
            description = """
                    소셜 가입 직후 기본 닉네임이 없는 사용자의 닉네임을 최초 1회 등록합니다.
                    같은 닉네임으로 다시 요청하면 성공하며, 다른 닉네임으로 변경하는 기능은 별도 API로 제공합니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 등록 성공 또는 같은 요청 재시도 성공"),
            @ApiResponse(responseCode = "400", description = "닉네임 검증 실패", content = @Content(examples = @ExampleObject(value = """
                    { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                    """))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """))),
            @ApiResponse(responseCode = "409", description = "이미 다른 닉네임으로 온보딩 완료", content = @Content(examples = @ExampleObject(value = """
                    { "code": "ONBOARDING_ALREADY_COMPLETED", "status": 409 }
                    """)))
    })
    public AuthUserResponse completeOnboarding(
            @Parameter(hidden = true)
            @CurrentMember(onboardingRequired = false) AuthenticatedMember member,
            @Valid @RequestBody CompleteOnboardingRequest request
    ) {
        return AuthUserResponse.from(memberOnboardingService.complete(member.userId(), request.nickname()));
    }

    @PatchMapping("/nickname")
    @Operation(
            summary = "기본 닉네임 수정",
            description = """
                    현재 사용자의 기본 닉네임을 수정합니다.
                    모임 안에서 이미 사용 중인 방장·회원 참여자 닉네임은 변경하지 않습니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기본 닉네임 수정 성공"),
            @ApiResponse(responseCode = "400", description = "닉네임 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(
                    responseCode = "403",
                    description = "최초 닉네임 온보딩이 완료되지 않은 사용자입니다. 먼저 PUT /api/users/me/onboarding으로 기본 닉네임을 등록해야 합니다.",
                    content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = """
                            { "code": "ONBOARDING_REQUIRED", "status": 403 }
                            """))
            )
    })
    public AuthUserResponse updateNickname(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        return AuthUserResponse.from(memberOnboardingService.updateNickname(member.userId(), request.nickname()));
    }

    @PatchMapping("/profile-color")
    @Operation(
            summary = "기본 프로필 색상 수정",
            description = "현재 회원의 기본 프로필 색상을 GRAY, RED, PURPLE, ORANGE 중 하나로 변경합니다. 현재는 색상 프로필만 지원합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기본 프로필 색상 수정 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 색상 또는 요청 본문 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(
                    responseCode = "403",
                    description = "최초 닉네임 온보딩이 완료되지 않은 사용자입니다.",
                    content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = """
                            { "code": "ONBOARDING_REQUIRED", "status": 403 }
                            """))
            )
    })
    public AuthUserResponse updateProfileColor(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody UpdateProfileColorRequest request
    ) {
        return AuthUserResponse.from(memberOnboardingService.updateProfileColor(member.userId(), request.color()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "회원 탈퇴",
            description = """
                    현재 회원을 탈퇴 처리하고 본인이 생성한 모든 모임과 개인 소유 데이터를 삭제합니다.
                    별도 소셜 로그인 없이 저장된 연결 정보로 Apple 토큰 철회 또는 Kakao 연결 해제를 완료합니다.
                    방장인 모임은 참여자·일정 후보·일정 가능 정보·모임 출발지 검색 이력·커버 이미지까지 삭제합니다.
                    다른 회원이 생성한 모임에서는 본인의 참여 행과 일정·출발지 정보를 모두 삭제합니다.
                    따라서 탈퇴 회원은 해당 모임의 참여자 목록, 인원 수, 일정 및 장소 계산에 포함되지 않습니다.
                    닉네임 온보딩을 완료하지 않은 회원도 탈퇴할 수 있습니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "소셜 연결 해제 및 회원 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(examples = @ExampleObject(value = """
                    { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                    """))),
            @ApiResponse(responseCode = "503", description = "소셜 연결 해제를 완료할 수 없음. 로컬 계정은 유지됩니다.", content = @Content(examples = @ExampleObject(value = """
                    { "code": "SOCIAL_LOGIN_UNAVAILABLE", "status": 503 }
                    """))),
            @ApiResponse(responseCode = "500", description = "활성 계정의 소셜 연결 정보가 없음", content = @Content(examples = @ExampleObject(value = """
                    { "code": "COMMON_INTERNAL_SERVER_ERROR", "status": 500 }
                    """)))
    })
    public void withdraw(
            @Parameter(hidden = true)
            @CurrentMember(onboardingRequired = false) AuthenticatedMember member
    ) {
        memberWithdrawalService.withdraw(member.userId());
    }
}
