package com.moyeo.controller.feedback;

import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.feedback.FeedbackService;
import com.moyeo.service.member.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedbacks")
@Tag(name = "Feedback", description = "사용자 피드백 API")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {
    private final FeedbackService feedbackService;
    public FeedbackController(FeedbackService feedbackService) { this.feedbackService = feedbackService; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "피드백 제출", description = "현재 로그인한 회원의 사용자 식별자와 함께 피드백을 저장합니다. 사용자 식별자는 요청 본문으로 받지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "피드백 제출 성공"),
            @ApiResponse(responseCode = "400", description = "피드백 내용 검증 실패", content = @Content(examples = @ExampleObject(value = "{ \"code\": \"COMMON_VALIDATION_FAILED\", \"status\": 400 }"))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content(examples = @ExampleObject(value = "{ \"code\": \"AUTHENTICATION_REQUIRED\", \"status\": 401 }"))),
            @ApiResponse(responseCode = "403", description = "닉네임 온보딩 필요", content = @Content(examples = @ExampleObject(value = "{ \"code\": \"ONBOARDING_REQUIRED\", \"status\": 403 }")))
    })
    public FeedbackResponse create(@Parameter(hidden = true) @CurrentMember AuthenticatedMember member, @Valid @RequestBody CreateFeedbackRequest request) {
        return FeedbackResponse.from(feedbackService.create(member.userId(), request.content()));
    }

    @GetMapping
    @Operation(summary = "내 피드백 이력 조회", description = "현재 로그인한 회원이 제출한 피드백만 최신 제출 순으로 반환합니다. 제출 이력이 없으면 빈 배열을 반환합니다.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "피드백 이력 조회 성공"), @ApiResponse(responseCode = "401", description = "인증 필요"), @ApiResponse(responseCode = "403", description = "닉네임 온보딩 필요") })
    public FeedbackListResponse findMyFeedbacks(@Parameter(hidden = true) @CurrentMember AuthenticatedMember member) {
        return FeedbackListResponse.from(feedbackService.findMyFeedbacks(member.userId()));
    }
}
