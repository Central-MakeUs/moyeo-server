package com.moyeo.service.member;

import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.AppleRevokeTask;
import com.moyeo.repository.member.AppleRevokeTaskRepository;
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
public class AppleRevokeProcessor {

    private static final Logger log = LoggerFactory.getLogger(AppleRevokeProcessor.class);
    private final AppleRevokeTaskRepository taskRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final AppleLoginService appleLoginService;
    private final TransactionTemplate transactionTemplate;
    private final Duration retryDelay;
    private final Duration maxRetryDelay;

    public AppleRevokeProcessor(
            AppleRevokeTaskRepository taskRepository,
            SocialAccountRepository socialAccountRepository,
            AppleLoginService appleLoginService,
            PlatformTransactionManager transactionManager,
            @Value("${moyeo.apple-revoke.retry-delay:1m}") Duration retryDelay,
            @Value("${moyeo.apple-revoke.max-retry-delay:1h}") Duration maxRetryDelay
    ) {
        if (retryDelay.isNegative() || retryDelay.isZero() || maxRetryDelay.compareTo(retryDelay) < 0) {
            throw new IllegalArgumentException("Invalid Apple revoke retry configuration");
        }
        this.taskRepository = taskRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.appleLoginService = appleLoginService;
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
            log.error("Could not load pending Apple revoke tasks. failureType={}", exception.getClass().getSimpleName());
        }
    }

    public boolean process(Long taskId) {
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                AppleRevokeTask task = taskRepository.findByIdForUpdate(taskId).orElse(null);
                if (task == null) return true;
                LocalDateTime now = LocalDateTime.now();
                if (task.getNextAttemptAt().isAfter(now)) return false;

                // A stale task must never disconnect a new active service membership.
                if (socialAccountRepository.findByProviderAndProviderUserId(AuthProvider.APPLE, task.getProviderUserId()).isPresent()) {
                    defer(task, now);
                    log.error("Apple revoke task conflicts with an active social account; operator review required. taskId={}", taskId);
                    return false;
                }

                try {
                    appleLoginService.disconnectStoredAuthorization(task.getProviderUserId(),
                            task.getProviderRefreshTokenCiphertext(), task.getAppleRefreshTokenClient());
                } catch (RuntimeException exception) {
                    defer(task, LocalDateTime.now());
                    log.warn("Apple revoke deferred. taskId={}, attempt={}, failureType={}",
                            taskId, task.getAttemptCount(), exception.getClass().getSimpleName());
                    if (task.getAttemptCount() >= 5) {
                        log.error("Repeated Apple revoke failure; operator review required. taskId={}, attempt={}",
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
            log.error("Could not confirm Apple revoke completion. taskId={}, failureType={}",
                    taskId, exception.getClass().getSimpleName());
            return false;
        }
    }

    private void defer(AppleRevokeTask task, LocalDateTime now) {
        Duration delay = retryDelay.multipliedBy(1L << Math.min(task.getAttemptCount(), 10));
        task.recordFailure(now, delay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : delay);
    }
}
