package com.moyeo.controller;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "서버 현재 시각 응답")
public record ServerTimeResponse(
        @Schema(
                description = "서버가 UTC 기준으로 반환한 현재 시각(ISO-8601)",
                example = "2026-07-17T02:30:45.123Z"
        )
        Instant serverTime
) {

    public static ServerTimeResponse now() {
        return new ServerTimeResponse(Instant.now());
    }
}
