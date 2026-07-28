package com.moyeo.service.meeting;

import java.time.LocalDateTime;
import java.util.List;

public record MyMeetingListResult(List<Item> planningMeetings, List<Item> confirmedMeetings) {
    public record Item(Long meetingId, String name, String coverImageUrl, String role, int participantCount,
                       int maxParticipants, String deadlineStatus, LocalDateTime deadlineAt,
                       LocalDateTime confirmedAt, LocalDateTime scheduledAt, String confirmedPlaceName) { }
}
