package com.moyeo.controller.member;

import com.moyeo.controller.auth.AuthUserResponse;
import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberOnboardingService;
import com.moyeo.service.member.MemberWithdrawalService;
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

    public MemberController(
            MemberOnboardingService memberOnboardingService,
            MemberWithdrawalService memberWithdrawalService
    ) {
        this.memberOnboardingService = memberOnboardingService;
        this.memberWithdrawalService = memberWithdrawalService;
    }

    @PutMapping("/onboarding")
    @Operation(
            summary = "최초 닉네임 등록",
            description = """
                    소셜 가입 직후 온보딩이 끝나지 않은 사용자의 닉네임을 최초 1회 등록합니다.
                    같은 닉네임으로 다시 요청하면 성공하며, 다른 닉네임으로 변경하는 기능은 추후 별도 API로 제공합니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 등록 성공 또는 같은 요청의 재시도 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "닉네임 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 없음, 만료 또는 유효하지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 다른 닉네임으로 온보딩 완료",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "ONBOARDING_ALREADY_COMPLETED", "status": 409 }
                            """))
            )
    })
    public AuthUserResponse completeOnboarding(
            @Parameter(hidden = true)
            @CurrentMember(onboardingRequired = false) AuthenticatedMember member,
            @Valid @RequestBody CompleteOnboardingRequest request
    ) {
        return AuthUserResponse.from(memberOnboardingService.complete(member.userId(), request.nickname()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "회원 탈퇴",
            description = """
                    현재 회원을 탈퇴 처리하고 본인이 생성한 모든 모임과 개인 소유 데이터를 삭제합니다.
                    별도 소셜 재로그인 없이 저장된 연결 정보로 Apple 토큰 철회 또는 Kakao 연결 해제를 완료합니다.
                    제공자 연결 해제에 실패하면 로컬 계정은 유지됩니다.
                    다른 회원이 생성한 모임의 참여 기록은 유지되며 참가자 조회에서 탈퇴 회원으로 표시됩니다.
                    닉네임 온보딩을 완료하지 않은 회원도 탈퇴할 수 있습니다.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "소셜 연결 해제 및 회원 탈퇴 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 없음, 만료 또는 유효하지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "AUTHENTICATION_REQUIRED", "status": 401 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "저장된 Apple 토큰을 사용할 수 없거나 소셜 연결 해제를 완료할 수 없음. 로컬 계정은 유지됩니다.",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "SOCIAL_LOGIN_UNAVAILABLE", "status": 503 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "활성 운영 계정의 소셜 연결 정보가 일관되지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON_INTERNAL_SERVER_ERROR", "status": 500 }
                            """))
            )
    })
    public void withdraw(
            @Parameter(hidden = true)
            @CurrentMember(onboardingRequired = false) AuthenticatedMember member
    ) {
        memberWithdrawalService.withdraw(member.userId());
    }
}
