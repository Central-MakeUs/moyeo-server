package com.moyeo.service.meeting;

public record MeetingParticipantNicknameResult(
        Long meetingId,
        Long participantId,
        Long userId,
        String nickname
) {
}
