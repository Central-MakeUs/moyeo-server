package com.moyeo.service.feedback;

import com.moyeo.domain.feedback.Feedback;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.feedback.FeedbackRepository;
import com.moyeo.repository.member.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository; this.userRepository = userRepository;
    }
    @Transactional
    public FeedbackResult create(Long userId, String content) {
        return FeedbackResult.from(feedbackRepository.save(new Feedback(findActiveUserForUpdate(userId), content)));
    }
    @Transactional(readOnly = true)
    public List<FeedbackResult> findMyFeedbacks(Long userId) {
        return feedbackRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream().map(FeedbackResult::from).toList();
    }
    private User findActiveUserForUpdate(Long userId) {
        return userRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));
    }
}
