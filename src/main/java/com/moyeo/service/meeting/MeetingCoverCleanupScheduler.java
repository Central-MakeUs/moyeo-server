package com.moyeo.service.meeting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class MeetingCoverCleanupScheduler {

    private final MeetingCoverCleanupProcessor cleanupProcessor;
    private final Duration orphanGracePeriod;

    public MeetingCoverCleanupScheduler(
            MeetingCoverCleanupProcessor cleanupProcessor,
            @Value("${moyeo.meeting-cover.orphan-grace-period:24h}") Duration orphanGracePeriod
    ) {
        this.cleanupProcessor = cleanupProcessor;
        this.orphanGracePeriod = orphanGracePeriod;
    }

    @Scheduled(
            fixedDelayString = "${moyeo.meeting-cover.cleanup-retry-delay:5m}",
            initialDelayString = "${moyeo.meeting-cover.cleanup-retry-delay:5m}"
    )
    public void retryPendingTasks() {
        cleanupProcessor.processPending();
    }

    @Scheduled(
            fixedDelayString = "${moyeo.meeting-cover.orphan-scan-delay:24h}",
            initialDelayString = "${moyeo.meeting-cover.orphan-scan-delay:24h}"
    )
    public void deleteOrphanedObjects() {
        cleanupProcessor.processOrphans(Instant.now().minus(orphanGracePeriod));
    }
}
