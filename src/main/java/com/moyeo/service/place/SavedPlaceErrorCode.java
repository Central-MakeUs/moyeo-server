package com.moyeo.service.place;

import com.moyeo.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum SavedPlaceErrorCode implements ErrorCode {

    SAVED_PLACE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SAVED_PLACE_NOT_FOUND",
            "saved-place-not-found",
            "저장 장소 없음",
            "해당 저장 장소를 찾을 수 없습니다."
    );

    private static final String TYPE_PREFIX = "urn:moyeo:problem:";

    private final HttpStatus status;
    private final String code;
    private final URI type;
    private final String title;
    private final String detail;

    SavedPlaceErrorCode(HttpStatus status, String code, String type, String title, String detail) {
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
