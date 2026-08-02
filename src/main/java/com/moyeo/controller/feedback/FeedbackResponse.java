package com.moyeo.controller.feedback;

import com.moyeo.service.feedback.FeedbackResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자가 제출한 피드백입니다.")
public record FeedbackResponse(
        @Schema(description = "피드백 ID", example = "1") Long id,
        @Schema(description = "제출 내용", example = "일정 후보를 복사하는 기능이 있으면 좋겠어요.") String content,
        @Schema(description = "제출 시각", example = "2026-08-02T14:30:00") LocalDateTime createdAt
) {
    public static FeedbackResponse from(FeedbackResult result) { return new FeedbackResponse(result.id(), result.content(), result.createdAt()); }
}
