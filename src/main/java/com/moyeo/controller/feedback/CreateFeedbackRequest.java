package com.moyeo.controller.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 피드백 등록 요청입니다. 내용 최대 1,000자는 임시 MVP 제한입니다.")
public record CreateFeedbackRequest(
        @Schema(description = "서비스에 전달할 의견입니다. 최대 1,000자는 임시 MVP 제한입니다.", example = "일정 후보를 복사하는 기능이 있으면 좋겠어요.", maxLength = 1000)
        @NotBlank @Size(max = 1000) String content
) {
    public CreateFeedbackRequest { content = content == null ? null : content.strip(); }
}
