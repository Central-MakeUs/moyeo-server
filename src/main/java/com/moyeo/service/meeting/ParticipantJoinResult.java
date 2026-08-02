package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;

public record ParticipantJoinResult(
        Long meetingId,
        Long participantId,
        Long userId,
        String nickname,
        String participantType
) {

    public static ParticipantJoinResult from(Meeting meeting, MeetingParticipant participant) {
        return new ParticipantJoinResult(
                meeting.getId(),
                participant.getId(),
                participant.getUser() == null ? null : participant.getUser().getId(),
                participant.getNickname(),
                participant.getParticipantType().name()
        );
    }
}
