package com.moyeo.service.room;

import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;

public record ParticipantJoinResult(
        Long roomId,
        Long participantId,
        String nickname,
        String participantType
) {

    public static ParticipantJoinResult from(Room room, RoomParticipant participant) {
        return new ParticipantJoinResult(
                room.getId(),
                participant.getId(),
                participant.getNickname(),
                participant.getParticipantType().name()
        );
    }
}
