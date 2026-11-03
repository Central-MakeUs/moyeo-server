package com.moyeo.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "apple_revoke_tasks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_apple_revoke_tasks_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_apple_revoke_tasks_provider_user", columnNames = "provider_user_id")
        },
        indexes = @Index(name = "idx_apple_revoke_tasks_due", columnList = "next_attempt_at,id"))
@Comment("탈퇴 커밋 후 완료할 애플 연결 해제 작업. 성공 시 삭제")
public class AppleRevokeTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @Comment("탈퇴한 원래 서비스 사용자 ID")
    private Long userId;

    @Column(name = "provider_user_id", nullable = false, length = 191)
    @Comment("연결 해제할 애플 사용자 ID. 복호화 인증에 사용하는 sub")
    private String providerUserId;

    @Column(name = "provider_refresh_token_ciphertext", nullable = false, length = 2048)
    @Comment("철회 완료까지 보관하는 암호화된 Apple refresh token")
    private String providerRefreshTokenCiphertext;

    @Enumerated(EnumType.STRING)
    @Column(name = "apple_refresh_token_client", nullable = false, length = 20)
    private AppleRefreshTokenClient appleRefreshTokenClient;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    protected AppleRevokeTask() {
    }

    public AppleRevokeTask(Long userId, String providerUserId, String ciphertext, AppleRefreshTokenClient client) {
        this.userId = userId;
        this.providerUserId = providerUserId;
        if (ciphertext == null || ciphertext.isBlank()) throw new IllegalArgumentException("Apple revoke token is required");
        this.providerRefreshTokenCiphertext = ciphertext;
        this.appleRefreshTokenClient = client == null ? AppleRefreshTokenClient.WEB : client;
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
    public String getProviderRefreshTokenCiphertext() { return providerRefreshTokenCiphertext; }
    public AppleRefreshTokenClient getAppleRefreshTokenClient() { return appleRefreshTokenClient; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
}
