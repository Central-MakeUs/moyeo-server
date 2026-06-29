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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "room_schedule_candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_schedule_candidates_room_date",
                        columnNames = {"room_id", "candidate_date"}
                )
        }
)
public class RoomScheduleCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_room_schedule_candidates_room"))
    private Room room;

    @Column(name = "candidate_date", nullable = false)
    private LocalDate candidateDate;

    protected RoomScheduleCandidate() {
    }

    public RoomScheduleCandidate(Room room, LocalDate candidateDate) {
        this.room = room;
        this.candidateDate = candidateDate;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getCandidateDate() {
        return candidateDate;
    }
}
