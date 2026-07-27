package com.moyeo.controller.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.moyeo.auth.OAuthRedirectTarget;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Apple 로그인 요청")
public record AppleLoginRequest(
        @Schema(description = "Apple이 프론트 콜백에 전달한 일회용 로그인 코드", example = "c123...")
        @NotBlank
        @Size(max = 4096)
        String code,

        @Schema(description = "프론트가 Apple 로그인 요청 전에 생성한 nonce 원문", example = "YpV5...")
        @NotBlank
        @Size(max = 255)
        String nonce,

        @Schema(description = "서버에 등록된 Apple 콜백 환경. local은 Apple 웹 로그인에서 지원하지 않습니다.", allowableValues = {"dev", "prod"}, example = "dev")
        @NotNull
        OAuthRedirectTarget redirectTarget
) {
    @JsonIgnore
    @AssertTrue(message = "Apple 로그인에는 dev 또는 prod redirectTarget만 사용할 수 있습니다.")
    public boolean isSupportedRedirectTarget() {
        return redirectTarget != OAuthRedirectTarget.LOCAL;
    }
}
