package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.MeetingCoverCleanupTask;
import com.moyeo.repository.meeting.MeetingCoverCleanupTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class MeetingCoverCleanupProcessor {

    private static final Logger log = LoggerFactory.getLogger(MeetingCoverCleanupProcessor.class);

    private final MeetingCoverCleanupTaskRepository cleanupTaskRepository;
    private final MeetingCoverStorage meetingCoverStorage;
    private final TransactionTemplate transactionTemplate;

    public MeetingCoverCleanupProcessor(
            MeetingCoverCleanupTaskRepository cleanupTaskRepository,
            MeetingCoverStorage meetingCoverStorage,
            PlatformTransactionManager transactionManager
    ) {
        this.cleanupTaskRepository = cleanupTaskRepository;
        this.meetingCoverStorage = meetingCoverStorage;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void processPending() {
        process(cleanupTaskRepository.findTop100ByOrderByLastAttemptedAtAscIdAsc().stream()
                .map(MeetingCoverCleanupTask::getId)
                .toList());
    }

    public void process(List<Long> taskIds) {
        for (Long taskId : taskIds) {
            try {
                processOne(taskId);
            } catch (RuntimeException exception) {
                log.warn("Failed to process meeting cover cleanup task. taskId={}", taskId, exception);
            }
        }
    }

    private void processOne(Long taskId) {
        transactionTemplate.executeWithoutResult(status ->
                cleanupTaskRepository.findByIdForUpdate(taskId).ifPresent(task -> {
                    try {
                        meetingCoverStorage.delete(task.getObjectKey());
                        cleanupTaskRepository.delete(task);
                    } catch (RuntimeException exception) {
                        task.recordFailure();
                        log.warn(
                                "Failed to delete meeting cover cleanup task. objectKey={}, attempt={}",
                                task.getObjectKey(),
                                task.getAttemptCount(),
                                exception
                        );
                    }
                })
        );
    }
}
