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

@Entity
@Comment("모임 참여자 일정 가능 날짜")
@Table(
        name = "meeting_participant_schedule_date_availabilities",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_meeting_participant_schedule_date_availabilities_date",
                        columnNames = {"participant_id", "schedule_candidate_id"}
                )
        }
)
public class MeetingParticipantScheduleDateAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("참여자 일정 가능 날짜 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "participant_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_meeting_participant_schedule_date_availabilities_participant")
    )
    @Comment("가능 날짜를 입력한 참여자 ID")
    private MeetingParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "schedule_candidate_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_meeting_participant_schedule_date_availabilities_candidate")
    )
    @Comment("선택한 일정 후보 날짜 ID")
    private MeetingScheduleCandidate scheduleCandidate;

    @Column(nullable = false)
    @Comment("가능 날짜 생성 일시")
    private LocalDateTime createdAt;

    protected MeetingParticipantScheduleDateAvailability() {
    }

    public MeetingParticipantScheduleDateAvailability(
            MeetingParticipant participant,
            MeetingScheduleCandidate scheduleCandidate
    ) {
        this.participant = participant;
        this.scheduleCandidate = scheduleCandidate;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public MeetingParticipant getParticipant() {
        return participant;
    }

    public MeetingScheduleCandidate getScheduleCandidate() {
        return scheduleCandidate;
    }
}
