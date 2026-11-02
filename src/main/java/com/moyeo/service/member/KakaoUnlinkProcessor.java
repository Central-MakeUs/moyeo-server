package com.moyeo.service.member;

import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.KakaoUnlinkTask;
import com.moyeo.repository.member.KakaoUnlinkTaskRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class KakaoUnlinkProcessor {

    private static final Logger log = LoggerFactory.getLogger(KakaoUnlinkProcessor.class);
    private final KakaoUnlinkTaskRepository taskRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final KakaoLoginService kakaoLoginService;
    private final TransactionTemplate transactionTemplate;
    private final Duration retryDelay;
    private final Duration maxRetryDelay;

    public KakaoUnlinkProcessor(
            KakaoUnlinkTaskRepository taskRepository,
            SocialAccountRepository socialAccountRepository,
            KakaoLoginService kakaoLoginService,
            PlatformTransactionManager transactionManager,
            @Value("${moyeo.kakao-unlink.retry-delay:1m}") Duration retryDelay,
            @Value("${moyeo.kakao-unlink.max-retry-delay:1h}") Duration maxRetryDelay
    ) {
        if (retryDelay.isNegative() || retryDelay.isZero() || maxRetryDelay.compareTo(retryDelay) < 0) {
            throw new IllegalArgumentException("Invalid Kakao unlink retry configuration");
        }
        this.taskRepository = taskRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.kakaoLoginService = kakaoLoginService;
        this.retryDelay = retryDelay;
        this.maxRetryDelay = maxRetryDelay;
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void processPending() {
        try {
            // Bound each scan on the existing single-thread scheduler.
            for (Long id : taskRepository.findDueIds(LocalDateTime.now(), PageRequest.of(0, 10))) {
                process(id);
            }
        } catch (RuntimeException exception) {
            log.error("Could not load pending Kakao unlink tasks. failureType={}", exception.getClass().getSimpleName());
        }
    }

    public boolean process(Long taskId) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                KakaoUnlinkTask task = taskRepository.findByIdForUpdate(taskId).orElse(null);
                if (task == null) return true;
                LocalDateTime now = LocalDateTime.now();
                if (task.getNextAttemptAt().isAfter(now)) return false;

                // A stale task must never disconnect a new active service membership.
                if (socialAccountRepository.findByProviderAndProviderUserId(AuthProvider.KAKAO, task.getProviderUserId()).isPresent()) {
                    defer(task, now);
                    log.error("Kakao unlink task conflicts with an active social account; operator review required. taskId={}", taskId);
                    return false;
                }

                try {
                    kakaoLoginService.disconnectStoredAccount(task.getProviderUserId());
                } catch (RuntimeException exception) {
                    defer(task, LocalDateTime.now());
                    log.warn("Kakao unlink deferred. taskId={}, attempt={}, failureType={}",
                            taskId, task.getAttemptCount(), exception.getClass().getSimpleName());
                    if (task.getAttemptCount() >= 5) {
                        log.error("Repeated Kakao unlink failure; operator review required. taskId={}, attempt={}",
                                taskId, task.getAttemptCount());
                    }
                    return false;
                }
                taskRepository.delete(task);
                taskRepository.flush();
                return true;
            }));
        } catch (RuntimeException exception) {
            // A failed completion commit leaves the task available for a later attempt.
            log.error("Could not confirm Kakao unlink completion. taskId={}, failureType={}",
                    taskId, exception.getClass().getSimpleName());
            return false;
        }
    }

    private void defer(KakaoUnlinkTask task, LocalDateTime now) {
        Duration delay = retryDelay.multipliedBy(1L << Math.min(task.getAttemptCount(), 10));
        task.recordFailure(now, delay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : delay);
    }
}
