package com.moyeo.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "kakao_unlink_tasks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_kakao_unlink_tasks_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_kakao_unlink_tasks_provider_user", columnNames = "provider_user_id")
        },
        indexes = @Index(name = "idx_kakao_unlink_tasks_due", columnList = "next_attempt_at,id"))
@Comment("탈퇴 커밋 후 완료할 카카오 연결 해제 작업. 성공 시 삭제")
public class KakaoUnlinkTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("탈퇴한 원래 서비스 사용자 ID")
    private Long userId;

    @Column(name = "provider_user_id", nullable = false, length = 191)
    @Comment("연결 해제할 카카오 사용자 ID. 토큰은 저장하지 않음")
    private String providerUserId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    protected KakaoUnlinkTask() {
    }

    public KakaoUnlinkTask(Long userId, String providerUserId) {
        this.userId = userId;
        this.providerUserId = providerUserId;
        // DATETIME(6) rounds nanoseconds; truncate to avoid a just-created task appearing in the future.
        this.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        this.nextAttemptAt = createdAt;
    }

    public void recordFailure(LocalDateTime now, Duration retryDelay) {
        if (attemptCount < Integer.MAX_VALUE) attemptCount++;
        lastAttemptedAt = now.truncatedTo(ChronoUnit.MICROS);
        nextAttemptAt = lastAttemptedAt.plus(retryDelay).truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getProviderUserId() { return providerUserId; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
}
