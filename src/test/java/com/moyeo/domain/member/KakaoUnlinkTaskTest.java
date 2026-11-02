package com.moyeo.domain.member;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoUnlinkTaskTest {
    @Test
    void immediateTaskAndRetryUseDatabaseMicrosecondPrecision() {
        KakaoUnlinkTask task = new KakaoUnlinkTask(1L, "123");
        assertThat(task.getNextAttemptAt().getNano() % 1000).isZero();
        LocalDateTime attempt = LocalDateTime.of(2026, 11, 2, 12, 0, 0, 123456789);
        task.recordFailure(attempt, Duration.ofMinutes(1));
        assertThat(task.getNextAttemptAt()).isEqualTo(attempt.plusMinutes(1).withNano(123456000));
        assertThat(task.getAttemptCount()).isEqualTo(1);
    }
}
