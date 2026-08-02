package com.moyeo.domain.feedback;

import com.moyeo.domain.member.User;
import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Comment("사용자 피드백")
@Table(name = "feedbacks", indexes = @Index(name = "idx_feedbacks_user_created", columnList = "user_id, created_at, id"))
public class Feedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("피드백 ID") private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_feedbacks_user"))
    @Comment("피드백을 제출한 서비스 사용자 ID") private User user;

    @Column(nullable = false, length = 1000)
    @Comment("사용자가 제출한 피드백 내용. 1,000자는 임시 MVP 제한") private String content;

    @Column(nullable = false)
    @Comment("피드백 제출 일시") private LocalDateTime createdAt;

    protected Feedback() {}
    public Feedback(User user, String content) { this.user = user; this.content = content.strip(); }
    @PrePersist void prePersist() { this.createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
