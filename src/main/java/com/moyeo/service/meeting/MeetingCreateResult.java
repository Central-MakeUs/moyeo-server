package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;

public record MeetingCreateResult(
        Long meetingId
) {

    public static MeetingCreateResult from(Meeting meeting) {
        return new MeetingCreateResult(meeting.getId());
    }
}
