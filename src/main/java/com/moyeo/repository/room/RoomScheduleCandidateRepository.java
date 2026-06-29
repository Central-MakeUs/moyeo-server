package com.moyeo.repository.room;

import com.moyeo.domain.room.RoomScheduleCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomScheduleCandidateRepository extends JpaRepository<RoomScheduleCandidate, Long> {

    List<RoomScheduleCandidate> findAllByRoomIdOrderByCandidateDateAsc(Long roomId);
}
