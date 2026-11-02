package com.moyeo.service.member;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KakaoUnlinkScheduler {
    private final KakaoUnlinkProcessor processor;

    public KakaoUnlinkScheduler(KakaoUnlinkProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${moyeo.kakao-unlink.scan-delay:1m}",
            initialDelayString = "${moyeo.kakao-unlink.scan-delay:1m}")
    public void retryPending() {
        processor.processPending();
    }
}
