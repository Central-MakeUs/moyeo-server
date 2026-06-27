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
            "A valid access token is required."
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
