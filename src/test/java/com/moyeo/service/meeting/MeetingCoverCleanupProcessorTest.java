package com.moyeo.service.meeting;

import com.moyeo.repository.meeting.MeetingCoverCleanupTaskRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("local")
class MeetingCoverCleanupProcessorTest {

    @Autowired
    private MeetingCoverCleanupProcessor cleanupProcessor;

    @Autowired
    private MeetingCoverCleanupTaskRepository cleanupTaskRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private MeetingCoverStorage meetingCoverStorage;

    @MockitoBean
    private MeetingRepository meetingRepository;

    @BeforeEach
    void setUp() {
        cleanupTaskRepository.deleteAll();
        reset(meetingCoverStorage);
    }

    @Test
    void failedImmediateDeletionIsQueuedAndRemovedAfterSuccessfulRetry() {
        String objectKey = "meeting-covers/retry.jpg";
        doThrow(new IllegalStateException("temporary failure"))
                .doNothing()
                .when(meetingCoverStorage)
                .delete(objectKey);

        cleanupProcessor.deleteOrEnqueue(objectKey);

        assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> assertThat(task.getObjectKey()).isEqualTo(objectKey));

        cleanupProcessor.processPending();

        verify(meetingCoverStorage, times(2)).delete(objectKey);
        assertThat(cleanupTaskRepository.count()).isZero();
    }

    @Test
    void scheduledDeletionRunsOutsideTheTaskTransactionAndRecordsFailures() {
        Long taskId = new TransactionTemplate(transactionManager).execute(status ->
                cleanupProcessor.createDeletionTask("meeting-covers/no-transaction.jpg"));
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(cleanupTaskRepository.existsById(taskId)).isTrue();
            throw new IllegalStateException("storage unavailable");
        }).when(meetingCoverStorage).delete("meeting-covers/no-transaction.jpg");
        cleanupProcessor.processPending();
        assertThat(cleanupTaskRepository.findById(taskId)).get().satisfies(task -> {
            assertThat(task.getAttemptCount()).isEqualTo(1);
            assertThat(task.getLastAttemptedAt()).isNotNull();
        });
    }

    @Test
    void concurrentCleanupMayRepeatDeletionButRemovesTheTaskOnce() throws Exception {
        String key = "meeting-covers/concurrent-cleanup.jpg";
        Long taskId = new TransactionTemplate(transactionManager).execute(status -> cleanupProcessor.createDeletionTask(key));
        var bothDeleting = new java.util.concurrent.CountDownLatch(2);
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            bothDeleting.countDown();
            assertThat(bothDeleting.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(meetingCoverStorage).delete(key);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> cleanupProcessor.process(List.of(taskId)));
            var second = executor.submit(() -> cleanupProcessor.process(List.of(taskId)));
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        verify(meetingCoverStorage, times(2)).delete(key);
        assertThat(cleanupTaskRepository.existsById(taskId)).isFalse();
    }

    @Test
    void successfulImmediateDeletionDoesNotCreateCleanupTask() {
        String objectKey = "meeting-covers/deleted.jpg";

        cleanupProcessor.deleteOrEnqueue(objectKey);

        verify(meetingCoverStorage).delete(objectKey);
        assertThat(cleanupTaskRepository.count()).isZero();
    }

    @Test
    void repeatedDeletionFailureKeepsSingleCleanupTask() {
        String objectKey = "meeting-covers/already-queued.jpg";
        doThrow(new IllegalStateException("temporary failure"))
                .when(meetingCoverStorage)
                .delete(objectKey);

        cleanupProcessor.deleteOrEnqueue(objectKey);
        cleanupProcessor.deleteOrEnqueue(objectKey);

        assertThat(cleanupTaskRepository.findAll())
                .singleElement()
                .satisfies(task -> assertThat(task.getObjectKey()).isEqualTo(objectKey));
    }

    @Test
    void deletionTaskRollsBackWithTheLocalDatabaseTransaction() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Long taskId = cleanupProcessor.createDeletionTask("meeting-covers/rollback.jpg");
            assertThat(cleanupTaskRepository.existsById(taskId)).isTrue();
            status.setRollbackOnly();
        });

        assertThat(cleanupTaskRepository.count()).isZero();
    }

    @Test
    void orphanScanDeletesOnlyOldUnreferencedObjects() {
        String orphanKey = "meeting-covers/orphan.jpg";
        String referencedKey = "meeting-covers/referenced.jpg";
        String recentKey = "meeting-covers/recent.jpg";
        Instant cutoff = Instant.parse("2026-07-27T00:00:00Z");
        when(meetingCoverStorage.list("meeting-covers/")).thenReturn(List.of(
                new MeetingCoverStorage.StoredObject(orphanKey, cutoff.minusSeconds(1)),
                new MeetingCoverStorage.StoredObject(referencedKey, cutoff.minusSeconds(1)),
                new MeetingCoverStorage.StoredObject(recentKey, cutoff)
        ));
        when(meetingRepository.existsByCoverImageKey(orphanKey)).thenReturn(false);
        when(meetingRepository.existsByCoverImageKey(referencedKey)).thenReturn(true);

        cleanupProcessor.processOrphans(cutoff);

        verify(meetingCoverStorage).delete(orphanKey);
        verify(meetingCoverStorage, never()).delete(referencedKey);
        verify(meetingCoverStorage, never()).delete(recentKey);
    }
}
