package com.moyeo.controller.member;

import com.moyeo.controller.auth.AuthUserResponse;
import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.MemberOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = "Member", description = "현재 사용자 정보 API")
public class MemberController {

    private final MemberOnboardingService memberOnboardingService;

    public MemberController(MemberOnboardingService memberOnboardingService) {
        this.memberOnboardingService = memberOnboardingService;
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
}
