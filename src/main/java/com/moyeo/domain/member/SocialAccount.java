package com.moyeo.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Comment("소셜 로그인 제공자 계정 연결 정보")
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_accounts_provider_user",
                        columnNames = {"provider", "provider_user_id"}
                ),
                @UniqueConstraint(
                        name = "uk_social_accounts_user_provider",
                        columnNames = {"user_id", "provider"}
                )
        }
)
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("소셜 계정 연결 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_social_accounts_user"))
    @Comment("연결된 서비스 사용자 ID")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("소셜 로그인 제공자")
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 191)
    @Comment("소셜 제공자가 발급한 사용자 식별자")
    private String providerUserId;

    @Column(length = 255)
    @Comment("소셜 제공자로부터 받은 이메일")
    private String email;

    @Column(name = "provider_refresh_token_ciphertext", length = 2048)
    @Comment("서버 암호화 키로 암호화한 제공자 refresh token. 현재 Apple만 저장")
    private String providerRefreshTokenCiphertext;

    @Enumerated(EnumType.STRING)
    @Column(name = "apple_refresh_token_client", length = 20)
    @Comment("Apple refresh token??諛쒓툒??client: WEB/ NATIVE. 湲곗〈 null? WEB濡?媛꾩＜")
    private AppleRefreshTokenClient appleRefreshTokenClient;

    @Column(nullable = false)
    @Comment("소셜 계정 연결 생성 일시")
    private LocalDateTime createdAt;

    protected SocialAccount() {
    }

    public SocialAccount(User user, AuthProvider provider, String providerUserId, String email) {
        this(user, provider, providerUserId, email, null);
    }

    public SocialAccount(
            User user,
            AuthProvider provider,
            String providerUserId,
            String email,
            String providerRefreshTokenCiphertext
    ) {
        this(user, provider, providerUserId, email, providerRefreshTokenCiphertext, null);
    }

    public SocialAccount(
            User user,
            AuthProvider provider,
            String providerUserId,
            String email,
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.providerRefreshTokenCiphertext = providerRefreshTokenCiphertext;
        this.appleRefreshTokenClient = appleRefreshTokenClient;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getProviderRefreshTokenCiphertext() {
        return providerRefreshTokenCiphertext;
    }

    public void updateProviderRefreshTokenCiphertext(String providerRefreshTokenCiphertext) {
        this.providerRefreshTokenCiphertext = providerRefreshTokenCiphertext;
    }

    public void updateAppleRefreshToken(
            String providerRefreshTokenCiphertext,
            AppleRefreshTokenClient appleRefreshTokenClient
    ) {
        this.providerRefreshTokenCiphertext = providerRefreshTokenCiphertext;
        this.appleRefreshTokenClient = appleRefreshTokenClient;
    }

    public AppleRefreshTokenClient getAppleRefreshTokenClient() {
        return appleRefreshTokenClient == null ? AppleRefreshTokenClient.WEB : appleRefreshTokenClient;
    }
}
