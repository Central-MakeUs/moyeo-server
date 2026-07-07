package com.moyeo.repository.room;

import com.moyeo.domain.room.RoomParticipantScheduleAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomParticipantScheduleAvailabilityRepository extends JpaRepository<RoomParticipantScheduleAvailability, Long> {

    long countByParticipantId(Long participantId);

    void deleteAllByParticipantId(Long participantId);
}
