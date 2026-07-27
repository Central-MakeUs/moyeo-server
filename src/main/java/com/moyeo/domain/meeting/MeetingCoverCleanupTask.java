package com.moyeo.domain.meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Comment("삭제에 실패한 모임 커버 이미지 정리 작업")
@Table(
        name = "meeting_cover_cleanup_tasks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_cover_cleanup_tasks_object_key",
                columnNames = "object_key"
        )
)
public class MeetingCoverCleanupTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("모임 커버 이미지 정리 작업 ID")
    private Long id;

    @Column(name = "object_key", nullable = false, length = 500)
    @Comment("삭제할 S3 객체 키")
    private String objectKey;

    @Column(name = "attempt_count", nullable = false)
    @Comment("S3 삭제 실패 횟수")
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    @Comment("정리 작업 생성 일시")
    private LocalDateTime createdAt;

    @Column(name = "last_attempted_at")
    @Comment("마지막 S3 삭제 시도 일시")
    private LocalDateTime lastAttemptedAt;

    protected MeetingCoverCleanupTask() {
    }

    public MeetingCoverCleanupTask(String objectKey) {
        this.objectKey = objectKey;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void recordFailure() {
        this.attemptCount++;
        this.lastAttemptedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastAttemptedAt() {
        return lastAttemptedAt;
    }
}
