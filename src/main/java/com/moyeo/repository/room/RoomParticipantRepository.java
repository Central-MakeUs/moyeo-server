package com.moyeo.repository.room;

import com.moyeo.domain.room.ParticipantType;
import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    long countByRoomId(Long roomId);

    boolean existsByRoomAndNicknameAndParticipantType(Room room, String nickname, ParticipantType participantType);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    Optional<RoomParticipant> findByIdAndRoomId(Long id, Long roomId);
}
