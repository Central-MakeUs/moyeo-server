package com.moyeo.domain.room;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rooms_invite_code", columnNames = "invite_code")
        }
)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rooms_host_user"))
    private User hostUser;

    @Column(nullable = false, length = 15)
    private String name;

    @Column(length = 100)
    private String description;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleMode scheduleMode;

    private LocalDateTime fixedScheduleAt;

    private LocalTime availableStartTime;

    private LocalTime availableEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlaceMode placeMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PlaceRecommendationStrategy placeRecommendationStrategy;

    @Column(length = 100)
    private String fixedPlaceName;

    @Column(length = 255)
    private String fixedPlaceAddress;

    @Column(nullable = false)
    private LocalDateTime deadlineAt;

    @Column(name = "invite_code", nullable = false, length = 20)
    private String inviteCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Room() {
    }

    public Room(
            User hostUser,
            String name,
            String description,
            Integer maxParticipants,
            ScheduleMode scheduleMode,
            LocalDateTime fixedScheduleAt,
            LocalTime availableStartTime,
            LocalTime availableEndTime,
            PlaceMode placeMode,
            PlaceRecommendationStrategy placeRecommendationStrategy,
            String fixedPlaceName,
            String fixedPlaceAddress,
            LocalDateTime deadlineAt,
            String inviteCode
    ) {
        this.hostUser = hostUser;
        this.name = name;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.scheduleMode = scheduleMode;
        this.fixedScheduleAt = fixedScheduleAt;
        this.availableStartTime = availableStartTime;
        this.availableEndTime = availableEndTime;
        this.placeMode = placeMode;
        this.placeRecommendationStrategy = placeRecommendationStrategy;
        this.fixedPlaceName = fixedPlaceName;
        this.fixedPlaceAddress = fixedPlaceAddress;
        this.deadlineAt = deadlineAt;
        this.inviteCode = inviteCode;
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

    public User getHostUser() {
        return hostUser;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public ScheduleMode getScheduleMode() {
        return scheduleMode;
    }

    public LocalDateTime getFixedScheduleAt() {
        return fixedScheduleAt;
    }

    public LocalTime getAvailableStartTime() {
        return availableStartTime;
    }

    public LocalTime getAvailableEndTime() {
        return availableEndTime;
    }

    public PlaceMode getPlaceMode() {
        return placeMode;
    }

    public PlaceRecommendationStrategy getPlaceRecommendationStrategy() {
        return placeRecommendationStrategy;
    }

    public String getFixedPlaceName() {
        return fixedPlaceName;
    }

    public String getFixedPlaceAddress() {
        return fixedPlaceAddress;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public String getInviteCode() {
        return inviteCode;
    }
}
