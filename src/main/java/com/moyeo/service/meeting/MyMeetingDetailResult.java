package com.moyeo.service.meeting;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record MyMeetingDetailResult(
        Long meetingId,
        String name,
        String description,
        String coverImageUrl,
        String hostNickname,
        LocalDate confirmedScheduleDate,
        LocalTime confirmedStartTime,
        LocalTime confirmedEndTime,
        String confirmedPlaceName,
        List<Participant> participants
) {

    public record Participant(Long participantId, String nickname, String participantType, boolean isMe) {
    }
}
