package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.MeetingCoverCleanupTask;
import com.moyeo.repository.meeting.MeetingCoverCleanupTaskRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;

@Service
public class MeetingCoverCleanupProcessor {

    private static final Logger log = LoggerFactory.getLogger(MeetingCoverCleanupProcessor.class);

    private final MeetingCoverCleanupTaskRepository cleanupTaskRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingCoverStorage meetingCoverStorage;
    private final TransactionTemplate transactionTemplate;

    public MeetingCoverCleanupProcessor(
            MeetingCoverCleanupTaskRepository cleanupTaskRepository,
            MeetingRepository meetingRepository,
            MeetingCoverStorage meetingCoverStorage,
            PlatformTransactionManager transactionManager
    ) {
        this.cleanupTaskRepository = cleanupTaskRepository;
        this.meetingRepository = meetingRepository;
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

    public void processOrphans(Instant olderThan) {
        try {
            meetingCoverStorage.list("meeting-covers/").stream()
                    .filter(object -> object.lastModifiedAt().isBefore(olderThan))
                    .map(MeetingCoverStorage.StoredObject::objectKey)
                    .filter(objectKey -> !meetingRepository.existsByCoverImageKey(objectKey))
                    .forEach(this::deleteOrEnqueue);
        } catch (RuntimeException exception) {
            log.warn("Failed to scan orphaned meeting cover objects.", exception);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long createDeletionTask(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return cleanupTaskRepository.saveAndFlush(new MeetingCoverCleanupTask(objectKey)).getId();
    }

    public void deleteOrEnqueue(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            meetingCoverStorage.delete(objectKey);
        } catch (RuntimeException deletionException) {
            enqueue(objectKey, deletionException);
        }
    }

    private void enqueue(String objectKey, RuntimeException deletionException) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    cleanupTaskRepository.saveAndFlush(new MeetingCoverCleanupTask(objectKey))
            );
            log.warn("Queued failed meeting cover deletion for retry. objectKey={}", objectKey, deletionException);
        } catch (DataIntegrityViolationException duplicateTaskException) {
            log.warn("Meeting cover deletion is already queued. objectKey={}", objectKey, deletionException);
        } catch (RuntimeException queueFailure) {
            log.error(
                    "Failed to queue meeting cover deletion. objectKey={}",
                    objectKey,
                    queueFailure
            );
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
