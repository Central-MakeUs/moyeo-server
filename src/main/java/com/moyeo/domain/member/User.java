package com.moyeo.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Comment("서비스 사용자")
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("서비스 사용자 ID")
    private Long id;

    @Column(length = 30)
    @Comment("사용자 기본 닉네임. null이면 소셜 가입 후 온보딩 미완료 또는 탈퇴 상태")
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("회원 기본 프로필 색상")
    private ProfileColor profileColor;

    @Column(nullable = false)
    @Comment("사용자 생성 일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("사용자 정보 수정 일시")
    private LocalDateTime updatedAt;

    @Comment("사용자 탈퇴/삭제 일시. null이면 활성 상태")
    private LocalDateTime deletedAt;

    protected User() {
    }

    public User(String nickname) {
        this.nickname = nickname;
        this.profileColor = ProfileColor.GRAY;
    }

    public static User pendingOnboarding() {
        return new User(null);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isOnboardingCompleted() {
        return nickname != null;
    }

    public ProfileColor getProfileColor() {
        return profileColor;
    }

    public void changeProfileColor(ProfileColor profileColor) {
        this.profileColor = profileColor;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void withdraw() {
        this.nickname = null;
        this.deletedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
