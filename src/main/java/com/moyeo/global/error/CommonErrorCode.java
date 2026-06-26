package com.moyeo.global.error;

import org.springframework.http.HttpStatus;

import java.net.URI;

public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_INVALID_REQUEST", "invalid-request", "잘못된 요청", "요청을 처리할 수 없습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON_VALIDATION_FAILED", "validation-failed", "요청 검증 실패", "요청 값이 올바르지 않습니다."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_MALFORMED_REQUEST", "malformed-request", "잘못된 요청 본문", "요청 본문을 읽을 수 없습니다."),
    MISSING_REQUIRED_VALUE(HttpStatus.BAD_REQUEST, "COMMON_MISSING_REQUIRED_VALUE", "missing-required-value", "필수 요청값 누락", "필수 요청값이 누락되었습니다."),
    TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "COMMON_TYPE_MISMATCH", "type-mismatch", "요청값 타입 불일치", "요청값의 타입이 올바르지 않습니다."),
    REQUEST_BINDING_FAILED(HttpStatus.BAD_REQUEST, "COMMON_REQUEST_BINDING_FAILED", "request-binding-failed", "요청 바인딩 실패", "요청값을 처리할 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_METHOD_NOT_ALLOWED", "method-not-allowed", "지원하지 않는 HTTP 메서드", "해당 HTTP 메서드는 지원하지 않습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON_UNSUPPORTED_MEDIA_TYPE", "unsupported-media-type", "지원하지 않는 미디어 타입", "해당 Content-Type은 지원하지 않습니다."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "COMMON_NOT_ACCEPTABLE", "not-acceptable", "지원하지 않는 응답 타입", "요청한 응답 형식을 제공할 수 없습니다."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_ENDPOINT_NOT_FOUND", "endpoint-not-found", "API 경로 없음", "요청한 API 경로를 찾을 수 없습니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "COMMON_PAYLOAD_TOO_LARGE", "payload-too-large", "요청 크기 초과", "요청 데이터의 크기가 허용 범위를 초과했습니다."),
    REQUEST_TIMEOUT(HttpStatus.SERVICE_UNAVAILABLE, "COMMON_REQUEST_TIMEOUT", "request-timeout", "요청 처리 시간 초과", "요청 처리 시간이 초과되었습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_SERVER_ERROR", "internal-server-error", "서버 오류", "서버 내부 오류가 발생했습니다.");

    private static final String TYPE_PREFIX = "urn:moyeo:problem:";

    private final HttpStatus status;
    private final String code;
    private final URI type;
    private final String title;
    private final String detail;

    CommonErrorCode(HttpStatus status, String code, String type, String title, String detail) {
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
