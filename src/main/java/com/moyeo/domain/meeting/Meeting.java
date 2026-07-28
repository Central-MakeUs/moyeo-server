package com.moyeo.domain.meeting;

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
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Comment("모임 방")
@Table(
        name = "meetings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meetings_invite_code", columnNames = "invite_code")
        }
)
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("모임 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_meetings_host_user"))
    @Comment("모임을 만든 방장 사용자 ID")
    private User hostUser;

    @Column(nullable = false, length = 15)
    @Comment("모임 이름")
    private String name;

    @Column(length = 100)
    @Comment("모임 설명")
    private String description;

    @Column(nullable = false)
    @Comment("최대 참여 인원. 방장 포함")
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Comment("모임 생성 유형: SCHEDULE_ONLY/PLACE_ONLY/SCHEDULE_AND_PLACE")
    private PlanningType planningType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("일정 설정 방식: VOTE/FIXED/NONE")
    private ScheduleMode scheduleMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_input_type", nullable = false, length = 20)
    @Comment("일정 참여 입력 유형: DATE_ONLY/DATE_AND_TIME/NONE")
    private ScheduleInputType scheduleInputType;

    @Comment("확정 일정. schedule_mode가 FIXED일 때 사용")
    private LocalDateTime fixedScheduleAt;

    @Comment("일정 투표 공통 시작 시간. schedule_mode가 VOTE일 때 사용")
    private LocalTime availableStartTime;

    @Comment("일정 투표 공통 종료 시간. schedule_mode가 VOTE일 때 사용")
    private LocalTime availableEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("장소 설정 방식: FIXED/RECOMMEND/NONE")
    private PlaceMode placeMode;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    @Comment("장소 추천 방식. place_mode가 RECOMMEND일 때 사용")
    private PlaceRecommendationStrategy placeRecommendationStrategy;

    @Column(length = 100)
    @Comment("확정 장소 이름. place_mode가 FIXED일 때 사용")
    private String fixedPlaceName;

    @Column(length = 255)
    @Comment("확정 장소 주소. place_mode가 FIXED일 때 사용")
    private String fixedPlaceAddress;

    @Column(length = 500)
    @Comment("S3에 보관하는 모임 커버 이미지 객체 키")
    private String coverImageKey;

    @Comment("모임 참여/응답 마감 일시. null이면 마감 없음")
    private LocalDateTime deadlineAt;

    @Column(name = "invite_code", nullable = false, length = 20)
    @Comment("초대 링크에 사용하는 고유 코드")
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_status", nullable = false, length = 20)
    private MeetingStatus status = MeetingStatus.PLANNING;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "confirmed_schedule_date")
    private LocalDate confirmedScheduleDate;

    @Column(name = "confirmed_start_time")
    private LocalTime confirmedStartTime;

    @Column(name = "confirmed_end_time")
    private LocalTime confirmedEndTime;

    @Column(name = "confirmed_place_name", length = 255)
    private String confirmedPlaceName;

    @Column(name = "confirmed_place_address", length = 255)
    private String confirmedPlaceAddress;

    @Column(name = "confirmed_place_latitude", precision = 10, scale = 7)
    private java.math.BigDecimal confirmedPlaceLatitude;

    @Column(name = "confirmed_place_longitude", precision = 10, scale = 7)
    private java.math.BigDecimal confirmedPlaceLongitude;

    @Column(name = "confirmed_commercial_area_code", length = 30)
    private String confirmedCommercialAreaCode;

    @Column(nullable = false)
    @Comment("모임 생성 일시")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Comment("모임 수정 일시")
    private LocalDateTime updatedAt;

    protected Meeting() {
    }

    public Meeting(
            User hostUser,
            String name,
            String description,
            Integer maxParticipants,
            PlanningType planningType,
            ScheduleMode scheduleMode,
            ScheduleInputType scheduleInputType,
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
        this.planningType = planningType;
        this.scheduleMode = scheduleMode;
        this.scheduleInputType = scheduleInputType;
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

    public PlanningType getPlanningType() {
        return planningType;
    }

    public ScheduleMode getScheduleMode() {
        return scheduleMode;
    }

    public ScheduleInputType getScheduleInputType() {
        return scheduleInputType;
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

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void changeCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
    }

    public void removeCoverImage() {
        this.coverImageKey = null;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public MeetingStatus getStatus() { return status; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDate getConfirmedScheduleDate() { return confirmedScheduleDate; }
    public LocalTime getConfirmedStartTime() { return confirmedStartTime; }
    public LocalTime getConfirmedEndTime() { return confirmedEndTime; }
    public String getConfirmedPlaceName() { return confirmedPlaceName; }
    public String getConfirmedPlaceAddress() { return confirmedPlaceAddress; }
    public java.math.BigDecimal getConfirmedPlaceLatitude() { return confirmedPlaceLatitude; }
    public java.math.BigDecimal getConfirmedPlaceLongitude() { return confirmedPlaceLongitude; }
    public String getConfirmedCommercialAreaCode() { return confirmedCommercialAreaCode; }

    public void confirmSchedule(LocalDate scheduleDate, LocalTime startTime, LocalTime endTime) {
        this.confirmedScheduleDate = scheduleDate;
        this.confirmedStartTime = startTime;
        this.confirmedEndTime = endTime;
    }

    public void confirmPlace(String placeName, String placeAddress, java.math.BigDecimal placeLatitude,
                             java.math.BigDecimal placeLongitude, String commercialAreaCode) {
        this.confirmedPlaceName = placeName;
        this.confirmedPlaceAddress = placeAddress;
        this.confirmedPlaceLatitude = placeLatitude;
        this.confirmedPlaceLongitude = placeLongitude;
        this.confirmedCommercialAreaCode = commercialAreaCode;
    }

    public void completeConfirmation() {
        this.status = MeetingStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }
}
