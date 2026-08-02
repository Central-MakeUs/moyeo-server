package com.moyeo.domain.meeting;

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

import java.math.BigDecimal;

@Entity
@Table(name = "meeting_place_recommendation_snapshots", uniqueConstraints = @UniqueConstraint(
        name = "uk_meeting_place_recommendation_snapshots_meeting_rank", columnNames = {"meeting_id", "rank"}))
public class MeetingPlaceRecommendationSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, foreignKey = @ForeignKey(name = "fk_meeting_place_recommendation_snapshots_meeting"))
    private Meeting meeting;
    @Column(nullable = false) private int rank;
    @Column(name = "area_code", nullable = false, length = 30) private String areaCode;
    @Column(name = "area_name", nullable = false, length = 255) private String areaName;
    @Column(name = "category_name", nullable = false, length = 30) private String categoryName;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal longitude;
    @Column(name = "gu_name", length = 40) private String guName;
    @Column(name = "dong_name", length = 40) private String dongName;
    @Column(name = "average_straight_distance_meters") private Long averageStraightDistanceMeters;
    @Column(name = "average_travel_time_seconds", nullable = false) private long averageTravelTimeSeconds;
    @Column(name = "max_travel_time_seconds", nullable = false) private long maxTravelTimeSeconds;

    protected MeetingPlaceRecommendationSnapshot() {}
    public MeetingPlaceRecommendationSnapshot(Meeting meeting, int rank, String areaCode, String areaName, String categoryName,
                                               BigDecimal latitude, BigDecimal longitude, String guName, String dongName,
                                               Long averageStraightDistanceMeters, long averageTravelTimeSeconds, long maxTravelTimeSeconds) {
        this.meeting = meeting; this.rank = rank; this.areaCode = areaCode; this.areaName = areaName;
        this.categoryName = categoryName; this.latitude = latitude; this.longitude = longitude;
        this.guName = guName; this.dongName = dongName;
        this.averageStraightDistanceMeters = averageStraightDistanceMeters;
        this.averageTravelTimeSeconds = averageTravelTimeSeconds; this.maxTravelTimeSeconds = maxTravelTimeSeconds;
    }
    public int getRank() { return rank; } public String getAreaCode() { return areaCode; } public String getAreaName() { return areaName; }
    public String getCategoryName() { return categoryName; } public BigDecimal getLatitude() { return latitude; } public BigDecimal getLongitude() { return longitude; }
    public String getGuName() { return guName; } public String getDongName() { return dongName; } public Long getAverageStraightDistanceMeters() { return averageStraightDistanceMeters; }
    public long getAverageTravelTimeSeconds() { return averageTravelTimeSeconds; } public long getMaxTravelTimeSeconds() { return maxTravelTimeSeconds; }
}
