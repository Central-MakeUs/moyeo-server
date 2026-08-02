package com.moyeo.domain.commercial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Comment("추천 상권별 지하철역·호선 매핑")
@Table(
        name = "commercial_area_station_lines",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_commercial_area_station_lines_area_station_line",
                columnNames = {"commercial_area_id", "station_name", "line_name"}
        )
)
public class CommercialAreaStationLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("상권 지하철역·호선 매핑 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "commercial_area_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_commercial_area_station_lines_area")
    )
    @Comment("추천 상권 내부 ID")
    private CommercialAreaEntity commercialArea;

    @Column(name = "station_name", nullable = false, length = 100)
    @Comment("카카오 검증 역명")
    private String stationName;

    @Column(name = "line_name", nullable = false, length = 100)
    @Comment("카카오 검증 호선명")
    private String lineName;

    @Column(name = "station_address", nullable = false, length = 255)
    @Comment("카카오 검증 역 주소")
    private String stationAddress;

    @Column(name = "station_latitude", nullable = false, precision = 18, scale = 15)
    @Comment("카카오 검증 역 WGS84 위도")
    private BigDecimal stationLatitude;

    @Column(name = "station_longitude", nullable = false, precision = 18, scale = 15)
    @Comment("카카오 검증 역 WGS84 경도")
    private BigDecimal stationLongitude;

    @Column(name = "distance_meters", nullable = false)
    @Comment("상권 중심과 역 좌표의 직선거리 미터")
    private Integer distanceMeters;

    protected CommercialAreaStationLineEntity() {
    }

    public CommercialAreaStationLineEntity(
            CommercialAreaEntity commercialArea,
            String stationName,
            String lineName,
            String stationAddress,
            BigDecimal stationLatitude,
            BigDecimal stationLongitude,
            Integer distanceMeters
    ) {
        this.commercialArea = commercialArea;
        this.stationName = stationName;
        this.lineName = lineName;
        this.stationAddress = stationAddress;
        this.stationLatitude = stationLatitude;
        this.stationLongitude = stationLongitude;
        this.distanceMeters = distanceMeters;
    }

    public Long getId() { return id; }
    public CommercialAreaEntity getCommercialArea() { return commercialArea; }
    public String getStationName() { return stationName; }
    public String getLineName() { return lineName; }
    public String getStationAddress() { return stationAddress; }
    public BigDecimal getStationLatitude() { return stationLatitude; }
    public BigDecimal getStationLongitude() { return stationLongitude; }
    public Integer getDistanceMeters() { return distanceMeters; }
}
