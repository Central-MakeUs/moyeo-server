package com.moyeo.service.meeting;

import com.moyeo.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

import java.net.URI;

public enum MeetingCoverErrorCode implements ErrorCode {

    MEETING_COVER_IMAGE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "MEETING_COVER_IMAGE_NOT_FOUND",
            "meeting-cover-image-not-found",
            "모임 커버 이미지 없음",
            "조회할 수 있는 모임 커버 이미지가 없습니다."
    ),
    MEETING_COVER_IMAGE_UNAVAILABLE(
            HttpStatus.SERVICE_UNAVAILABLE,
            "MEETING_COVER_IMAGE_UNAVAILABLE",
            "meeting-cover-image-unavailable",
            "모임 커버 이미지 서비스 이용 불가",
            "모임 커버 이미지 서비스를 일시적으로 이용할 수 없습니다."
    ),
    MEETING_COVER_IMAGE_MODIFICATION_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "MEETING_COVER_IMAGE_MODIFICATION_FORBIDDEN",
            "meeting-cover-image-modification-forbidden",
            "모임 커버 이미지 수정 권한 없음",
            "모임 방장만 커버 이미지를 수정하거나 삭제할 수 있습니다."
    );

    private static final String TYPE_PREFIX = "urn:moyeo:problem:";

    private final HttpStatus status;
    private final String code;
    private final URI type;
    private final String title;
    private final String detail;

    MeetingCoverErrorCode(HttpStatus status, String code, String type, String title, String detail) {
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
