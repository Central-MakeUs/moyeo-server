package com.moyeo.service.feedback;

import com.moyeo.domain.feedback.Feedback;
import java.time.LocalDateTime;

public record FeedbackResult(Long id, String content, LocalDateTime createdAt) {
    static FeedbackResult from(Feedback feedback) {
        return new FeedbackResult(feedback.getId(), feedback.getContent(), feedback.getCreatedAt());
    }
}
