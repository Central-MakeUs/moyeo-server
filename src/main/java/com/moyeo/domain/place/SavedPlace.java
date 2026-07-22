package com.moyeo.domain.place;

import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.domain.member.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Comment("회원 저장 장소")
@Table(
        name = "saved_places",
        indexes = @Index(
                name = "idx_saved_places_user_created",
                columnList = "user_id, created_at, id"
        )
)
public class SavedPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회원 저장 장소 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_saved_places_user")
    )
    @Comment("장소를 저장한 서비스 사용자 ID")
    private User user;

    @Column(nullable = false, length = 30)
    @Comment("회원이 입력한 장소 별칭")
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("검색 결과 유형: STATION/ADDRESS/PLACE")
    private DeparturePlaceType type;

    @Column(name = "display_name", nullable = false, length = 255)
    @Comment("검색 결과의 원본 표시명")
    private String displayName;

    @Column(nullable = false, length = 255)
    @Comment("대표 주소")
    private String address;

    @Column(name = "road_address", length = 255)
    @Comment("도로명주소")
    private String roadAddress;

    @Column(name = "jibun_address", length = 255)
    @Comment("지번주소")
    private String jibunAddress;

    @Column(nullable = false, precision = 18, scale = 15)
    @Comment("WGS84 위도")
    private BigDecimal latitude;

    @Column(nullable = false, precision = 18, scale = 15)
    @Comment("WGS84 경도")
    private BigDecimal longitude;

    @Column(nullable = false)
    @Comment("장소 저장 일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("저장 장소 수정 일시")
    private LocalDateTime updatedAt;

    protected SavedPlace() {
    }

    public SavedPlace(
            User user,
            String alias,
            DeparturePlaceType type,
            String displayName,
            String address,
            String roadAddress,
            String jibunAddress,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.user = user;
        this.alias = alias.strip();
        this.type = type;
        this.displayName = displayName.strip();
        this.address = address.strip();
        this.roadAddress = stripToNull(roadAddress);
        this.jibunAddress = stripToNull(jibunAddress);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void rename(String alias) {
        this.alias = alias.strip();
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

    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getAlias() {
        return alias;
    }

    public DeparturePlaceType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAddress() {
        return address;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public String getJibunAddress() {
        return jibunAddress;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
