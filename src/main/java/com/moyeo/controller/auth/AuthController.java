package com.moyeo.controller.auth;

import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.global.security.CurrentMember;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.member.RefreshTokenService;
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
    private final KakaoLoginService kakaoLoginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AppleLoginService appleLoginService,
            KakaoLoginService kakaoLoginService,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.appleLoginService = appleLoginService;
        this.kakaoLoginService = kakaoLoginService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/apple")
    @Operation(
            summary = "Apple 로그인",
            description = """
                    프론트가 Apple GET 콜백에서 받은 일회용 code, 로그인 요청 전에 만든 nonce, 콜백 환경 식별자를 전달합니다.
                    redirectTarget은 dev 또는 prod만 허용하며 URI 자체는 전달하지 않습니다.
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
        AuthenticatedMember member = appleLoginService.login(request.code(), request.nonce(), request.redirectTarget());
        return issueTokens(member);
    }

    @PostMapping("/apple/native")
    @Operation(summary = "Apple 네이티브 SDK 로그인", description = "네이티브 앱이 Apple SDK에서 받은 identityToken, authorizationCode, nonce를 전달합니다. 서버는 App ID audience와 nonce, 두 identity token의 동일한 sub를 검증한 뒤 Access Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Apple 네이티브 SDK 로그인 및 Access Token 발급 성공"),
            @ApiResponse(responseCode = "400", description = "identityToken, authorizationCode 또는 nonce 요청값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "Apple identity token 또는 authorization code 검증 실패"),
            @ApiResponse(responseCode = "503", description = "Apple 로그인 서비스 설정 미완료 또는 일시적 장애")
    })
    public AuthResponse loginAppleNative(@Valid @RequestBody AppleNativeLoginRequest request) {
        AuthenticatedMember member = appleLoginService.loginNative(
                request.identityToken(), request.authorizationCode(), request.nonce());
        return issueTokens(member);
    }

    @PostMapping("/kakao")
    @Operation(
            summary = "카카오 로그인",
            description = """
                    프론트엔드가 카카오 GET 콜백의 state를 검증한 뒤 일회용 code와 콜백 환경 식별자를 전달합니다.
                    redirectTarget은 local, dev, prod 중 서버에 등록된 값만 허용하며 URI 자체는 전달하지 않습니다.
                    서버가 카카오와 code를 교환하고 회원번호를 확인한 뒤 Moyeo Access Token을 발급합니다.
                    최초 로그인도 즉시 가입 처리하며 nickname은 null, onboardingCompleted는 false로 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카카오 로그인 및 Access Token 발급 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "code 요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "code가 유효하지 않거나 만료 또는 재사용됨",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "SOCIAL_LOGIN_FAILED", "status": 401 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "카카오 로그인 서비스의 일시적 장애 또는 서버 설정 미완료",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "SOCIAL_LOGIN_UNAVAILABLE", "status": 503 }
                            """))
            )
    })
    public AuthResponse loginKakao(@Valid @RequestBody KakaoLoginRequest request) {
        AuthenticatedMember member = kakaoLoginService.login(request.code(), request.redirectTarget());
        return issueTokens(member);
    }

    @PostMapping("/kakao/native")
    @Operation(
            summary = "카카오 네이티브 SDK 로그인",
            description = """
                    네이티브 앱이 카카오 SDK로 발급받은 Access Token을 전달합니다.
                    서버는 토큰으로 카카오 사용자 정보를 조회해 회원번호를 확인한 뒤 Moyeo Access Token을 발급합니다.
                    카카오 Access Token은 로그인 요청 처리 후 저장하지 않습니다.
                    브라우저 로그인은 별도 `POST /api/auth/kakao` API를 사용합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카카오 네이티브 SDK 로그인 및 Access Token 발급 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "accessToken 요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "카카오 Access Token이 유효하지 않거나 만료됨",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "SOCIAL_LOGIN_FAILED", "status": 401 }
                            """))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "카카오 로그인 서비스의 일시적 장애 또는 서버 설정 미완료",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "SOCIAL_LOGIN_UNAVAILABLE", "status": 503 }
                            """))
            )
    })
    public AuthResponse loginKakaoNative(@Valid @RequestBody KakaoNativeLoginRequest request) {
        AuthenticatedMember member = kakaoLoginService.loginWithAccessToken(request.accessToken());
        return issueTokens(member);
    }

    @PostMapping("/refresh")
    @Operation(summary = "로그인 연장", description = "저장된 갱신 토큰으로 새 Access Token과 갱신 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 연장 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "갱신 토큰 요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                            """))
            ),
            @ApiResponse(responseCode = "401", description = "갱신 토큰이 없거나 만료 또는 무효화됨")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenService.RefreshSession session = refreshTokenService.refresh(request.refreshToken());
        return AuthResponse.of(
                jwtTokenProvider.createAccessToken(session.member(), session.sessionId()),
                session.refreshToken(),
                session.member()
        );
    }

    @PostMapping("/logout")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(summary = "현재 기기 로그아웃", description = "현재 기기에 저장된 갱신 토큰과 Access Token을 즉시 무효화합니다. 다른 기기의 로그인은 유지됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "현재 기기 로그아웃 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "갱신 토큰 요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            { "code": "COMMON_VALIDATION_FAILED", "status": 400 }
                            """))
            ),
            @ApiResponse(responseCode = "401", description = "갱신 토큰이 없거나 만료 또는 무효화됨")
    })
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.logout(request.refreshToken());
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

    private AuthResponse issueTokens(AuthenticatedMember member) {
        RefreshTokenService.SessionToken session = refreshTokenService.issue(member.userId());
        return AuthResponse.of(
                jwtTokenProvider.createAccessToken(member, session.sessionId()),
                session.refreshToken(),
                member
        );
    }
}
