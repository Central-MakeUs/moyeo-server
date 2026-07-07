package com.moyeo.domain.room;

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
@Comment("모임 참여자 일정 가능 시간")
@Table(
        name = "room_participant_schedule_availabilities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_participant_schedule_availabilities_slot",
                        columnNames = {"participant_id", "schedule_candidate_id", "start_time", "end_time"}
                )
        }
)
public class RoomParticipantScheduleAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("참여자 일정 가능 시간 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "participant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_participant_schedule_availabilities_participant")
    )
    @Comment("일정 가능 시간을 입력한 참여자 ID")
    private RoomParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "schedule_candidate_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_participant_schedule_availabilities_candidate")
    )
    @Comment("일정 후보 날짜 ID")
    private RoomScheduleCandidate scheduleCandidate;

    @Column(name = "start_time", nullable = false)
    @Comment("가능 시간 시작")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @Comment("가능 시간 종료")
    private LocalTime endTime;

    @Column(nullable = false)
    @Comment("일정 가능 시간 생성 일시")
    private LocalDateTime createdAt;

    protected RoomParticipantScheduleAvailability() {
    }

    public RoomParticipantScheduleAvailability(
            RoomParticipant participant,
            RoomScheduleCandidate scheduleCandidate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.participant = participant;
        this.scheduleCandidate = scheduleCandidate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public RoomParticipant getParticipant() {
        return participant;
    }

    public RoomScheduleCandidate getScheduleCandidate() {
        return scheduleCandidate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
