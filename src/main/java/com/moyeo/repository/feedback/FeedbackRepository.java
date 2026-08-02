package com.moyeo.repository.feedback;

import com.moyeo.domain.feedback.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    void deleteAllByUserId(Long userId);
}
