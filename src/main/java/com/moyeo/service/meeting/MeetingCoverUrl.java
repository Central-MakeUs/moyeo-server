package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;

public final class MeetingCoverUrl {

    private MeetingCoverUrl() {
    }

    public static String from(Meeting meeting) {
        String coverImageKey = meeting.getCoverImageKey();
        if (coverImageKey == null) {
            return null;
        }
        String version = coverImageKey.substring(
                coverImageKey.lastIndexOf('/') + 1,
                coverImageKey.lastIndexOf('.')
        );
        return "/api/meetings/invitations/" + meeting.getInviteCode()
                + "/cover-image?v=" + version;
    }
}
