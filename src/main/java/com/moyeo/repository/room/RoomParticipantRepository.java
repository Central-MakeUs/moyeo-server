package com.moyeo.repository.room;

import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    long countByRoomId(Long roomId);

    boolean existsByRoomAndNickname(Room room, String nickname);

    Optional<RoomParticipant> findByIdAndRoomId(Long id, Long roomId);
}
