package com.moyeo.domain.commercial;

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

import java.math.BigDecimal;

@Entity
@Comment("추천 후보 상권 데이터")
@Table(
        name = "commercial_areas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_commercial_areas_source_external_code",
                columnNames = {"source", "external_code"}
        ),
        indexes = @Index(
                name = "idx_commercial_areas_source_type",
                columnList = "source, area_type"
        )
)
public class CommercialAreaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("상권 내부 ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Comment("상권 데이터 출처")
    private CommercialAreaSource source;

    @Column(name = "external_code", nullable = false, length = 30)
    @Comment("출처가 부여한 상권 코드")
    private String externalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_type", nullable = false, length = 30)
    @Comment("추천 대상 상권 유형")
    private CommercialAreaType areaType;

    @Column(name = "area_name", nullable = false, length = 255)
    @Comment("상권명")
    private String areaName;

    @Column(nullable = false, precision = 10, scale = 7)
    @Comment("상권 중심 WGS84 위도")
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    @Comment("상권 중심 WGS84 경도")
    private BigDecimal longitude;

    @Column(name = "district_code", length = 10)
    @Comment("시군구 코드")
    private String districtCode;

    @Column(name = "district_name", length = 40)
    @Comment("시군구명")
    private String districtName;

    @Column(name = "administrative_dong_code", length = 12)
    @Comment("행정동 코드")
    private String administrativeDongCode;

    @Column(name = "administrative_dong_name", length = 40)
    @Comment("행정동명")
    private String administrativeDongName;

    protected CommercialAreaEntity() {
    }

    public CommercialAreaEntity(
            CommercialAreaSource source,
            String externalCode,
            CommercialAreaType areaType,
            String areaName,
            BigDecimal latitude,
            BigDecimal longitude,
            String districtCode,
            String districtName,
            String administrativeDongCode,
            String administrativeDongName
    ) {
        this.source = source;
        this.externalCode = externalCode;
        this.areaType = areaType;
        this.areaName = areaName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.districtCode = districtCode;
        this.districtName = districtName;
        this.administrativeDongCode = administrativeDongCode;
        this.administrativeDongName = administrativeDongName;
    }

    public Long getId() {
        return id;
    }

    public CommercialAreaSource getSource() {
        return source;
    }

    public String getExternalCode() {
        return externalCode;
    }

    public CommercialAreaType getAreaType() {
        return areaType;
    }

    public String getAreaName() {
        return areaName;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public String getDistrictName() {
        return districtName;
    }

    public String getAdministrativeDongCode() {
        return administrativeDongCode;
    }

    public String getAdministrativeDongName() {
        return administrativeDongName;
    }
}
