package com.moyeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/time")
@Tag(name = "Time", description = "서버 시간 조회 API")
public class ServerTimeController {

    @GetMapping
    @Operation(
            summary = "서버 현재 시간 조회",
            description = "서버의 현재 시간을 UTC 기준 ISO-8601 형식으로 반환합니다. 클라이언트의 날짜 및 시간 판단 기준을 서버와 동기화할 때 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "서버 현재 시간 조회 성공")
    })
    public ServerTimeResponse getServerTime() {
        return ServerTimeResponse.now();
    }
}
