package com.moyeo.controller.feedback;

import com.moyeo.service.feedback.FeedbackResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "내 피드백 제출 이력 응답입니다.")
public record FeedbackListResponse(@Schema(description = "최신 제출 순 피드백 목록") List<FeedbackResponse> feedbacks) {
    public static FeedbackListResponse from(List<FeedbackResult> results) {
        return new FeedbackListResponse(results.stream().map(FeedbackResponse::from).toList());
    }
}
