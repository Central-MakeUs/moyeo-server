package com.moyeo.domain.departure;

import com.moyeo.departure.DeparturePlaceType;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

@Entity
@Comment("출발지 검색 결과 후보")
@Table(
        name = "departure_place_search_candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_departure_place_search_candidates_position",
                        columnNames = {"search_id", "position"}
                )
        }
)
public class DeparturePlaceSearchCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("출발지 검색 결과 후보 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "search_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_departure_place_search_candidates_search")
    )
    @Comment("출발지 검색 실행 ID")
    private DeparturePlaceSearch search;

    @Column(nullable = false)
    @Comment("클라이언트 응답 내 결과 순서. 1부터 시작")
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("검색 결과 유형: STATION/ADDRESS/PLACE")
    private DeparturePlaceType type;

    @Column(name = "display_name", length = 255)
    @Comment("검색 목록 표시명")
    private String displayName;

    @Column(length = 255)
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

    protected DeparturePlaceSearchCandidate() {
    }

    DeparturePlaceSearchCandidate(
            DeparturePlaceSearch search,
            int position,
            DeparturePlaceType type,
            String displayName,
            String address,
            String roadAddress,
            String jibunAddress,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.search = search;
        this.position = position;
        this.type = type;
        this.displayName = displayName;
        this.address = address;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public Integer getPosition() {
        return position;
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
}
