package com.moyeo.global.security;

import com.moyeo.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum AuthenticationErrorCode implements ErrorCode {

    AUTHENTICATION_REQUIRED(
            HttpStatus.UNAUTHORIZED,
            "AUTHENTICATION_REQUIRED",
            "authentication-required",
            "Authentication required",
            "Authentication is required."
    ),
    SOCIAL_LOGIN_FAILED(
            HttpStatus.UNAUTHORIZED,
            "SOCIAL_LOGIN_FAILED",
            "social-login-failed",
            "Social login failed",
            "The social login request could not be verified."
    ),
    SOCIAL_LOGIN_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "SOCIAL_LOGIN_UNAVAILABLE",
            "social-login-unavailable",
            "Social login unavailable",
            "The social login provider is temporarily unavailable."
    ),
    ONBOARDING_REQUIRED(
            HttpStatus.FORBIDDEN,
            "ONBOARDING_REQUIRED",
            "onboarding-required",
            "Onboarding required",
            "Nickname onboarding must be completed first."
    ),
    ONBOARDING_ALREADY_COMPLETED(
            HttpStatus.CONFLICT,
            "ONBOARDING_ALREADY_COMPLETED",
            "onboarding-already-completed",
            "Onboarding already completed",
            "Nickname onboarding has already been completed."
    );

    private static final String TYPE_PREFIX = "urn:moyeo:problem:";

    private final HttpStatus status;
    private final String code;
    private final URI type;
    private final String title;
    private final String detail;

    AuthenticationErrorCode(HttpStatus status, String code, String type, String title, String detail) {
        this.status = status;
        this.code = code;
        this.type = URI.create(TYPE_PREFIX + type);
        this.title = title;
        this.detail = detail;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public URI type() {
        return type;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String detail() {
        return detail;
    }
}
