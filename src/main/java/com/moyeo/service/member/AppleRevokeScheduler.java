package com.moyeo.service.member;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AppleRevokeScheduler {
    private final AppleRevokeProcessor processor;

    public AppleRevokeScheduler(AppleRevokeProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${moyeo.apple-revoke.scan-delay:1m}",
            initialDelayString = "${moyeo.apple-revoke.scan-delay:1m}")
    public void retryPending() {
        processor.processPending();
    }
}
