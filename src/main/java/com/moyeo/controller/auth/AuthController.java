package com.moyeo.controller.auth;

import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.global.security.CurrentMember;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "소셜 로그인 및 현재 사용자 조회 API")
public class AuthController {

    private final AppleLoginService appleLoginService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AppleLoginService appleLoginService, JwtTokenProvider jwtTokenProvider) {
        this.appleLoginService = appleLoginService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/apple")
    @Operation(
            summary = "Apple 로그인",
            description = """
                    프론트가 Apple GET 콜백에서 받은 일회용 code와 로그인 요청 전에 만든 nonce를 전달합니다.
                    서버가 Apple과 code를 교환하고 사용자 정보를 검증한 뒤 Moyeo Access Token을 발급합니다.
                    최초 로그인도 즉시 가입 처리되며 nickname은 null, onboardingCompleted는 false로 반환됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Apple 로그인 및 Access Token 발급 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "code 또는 nonce 요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "code가 유효하지 않거나 만료·재사용됐거나 Apple 응답 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "SOCIAL_LOGIN_FAILED", "status": 401 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Apple 로그인 서비스의 일시적 장애 또는 서버 설정 미완료",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "SOCIAL_LOGIN_UNAVAILABLE", "status": 503 }
                            """))
            )
    })
    public AuthResponse loginApple(@Valid @RequestBody AppleLoginRequest request) {
        AuthenticatedMember member = appleLoginService.login(request.code(), request.nonce());
        return AuthResponse.of(jwtTokenProvider.createAccessToken(member), member);
    }

    @GetMapping("/me")
    @Operation(
            summary = "현재 사용자 조회",
            description = "`Authorization: Bearer {accessToken}` 헤더로 현재 로그인 사용자를 조회합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 사용자 조회 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 없음, 만료 또는 유효하지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "AUTHENTICATION_REQUIRED",
                              "status": 401
                            }
                            """))
            )
    })
    public AuthUserResponse me(
            @Parameter(hidden = true)
            @CurrentMember(onboardingRequired = false) AuthenticatedMember member
    ) {
        return AuthUserResponse.from(member);
    }
}
