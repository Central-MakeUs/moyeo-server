package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;

public record HostParticipationResult(
        Long meetingId,
        String inviteCode,
        String invitePath
) {

    public static HostParticipationResult from(Meeting meeting) {
        return new HostParticipationResult(
                meeting.getId(),
                meeting.getInviteCode(),
                "/meetings/invitations/" + meeting.getInviteCode()
        );
    }
}
