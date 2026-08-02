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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Comment("모임 일정 후보 선택 가능 시간 스냅샷")
@Table(
        name = "meeting_schedule_candidate_availabilities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_meeting_schedule_candidate_availabilities_slot",
                        columnNames = {"schedule_candidate_id", "start_time", "end_time"}
                )
        }
)
public class MeetingScheduleCandidateAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "schedule_candidate_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_meeting_schedule_candidate_availabilities_candidate")
    )
    private MeetingScheduleCandidate scheduleCandidate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MeetingScheduleCandidateAvailability() {
    }

    public MeetingScheduleCandidateAvailability(
            MeetingScheduleCandidate scheduleCandidate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.scheduleCandidate = scheduleCandidate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public MeetingScheduleCandidate getScheduleCandidate() {
        return scheduleCandidate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
