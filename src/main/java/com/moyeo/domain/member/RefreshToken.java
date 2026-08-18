package com.moyeo.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash"),
        @UniqueConstraint(name = "uk_refresh_tokens_session_id", columnNames = "session_id")
})
@Comment("서비스 로그인 연장 토큰")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_tokens_user"))
    private User user;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(User user, String tokenHash, String sessionId, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.sessionId = sessionId;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isExpiredAt(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    public void rotate(String tokenHash, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
