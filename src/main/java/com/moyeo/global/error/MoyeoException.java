package com.moyeo.global.error;

import java.util.Objects;

public class MoyeoException extends RuntimeException {

    private final ErrorCode errorCode;

    public MoyeoException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode, "errorCode must not be null").detail());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
