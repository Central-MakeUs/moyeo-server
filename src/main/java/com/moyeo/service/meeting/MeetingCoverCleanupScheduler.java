package com.moyeo.service.meeting;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MeetingCoverCleanupScheduler {

    private final MeetingCoverCleanupProcessor cleanupProcessor;

    public MeetingCoverCleanupScheduler(MeetingCoverCleanupProcessor cleanupProcessor) {
        this.cleanupProcessor = cleanupProcessor;
    }

    @Scheduled(
            fixedDelayString = "${moyeo.meeting-cover.cleanup-retry-delay:5m}",
            initialDelayString = "${moyeo.meeting-cover.cleanup-retry-delay:5m}"
    )
    public void retryPendingTasks() {
        cleanupProcessor.processPending();
    }
}
