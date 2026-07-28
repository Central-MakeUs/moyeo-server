package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.MyMeetingListResult;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MyMeetingListResponse(List<Item> planningMeetings, List<Item> confirmedMeetings) {
    public static MyMeetingListResponse from(MyMeetingListResult result) {
        return new MyMeetingListResponse(result.planningMeetings().stream().map(Item::from).toList(), result.confirmedMeetings().stream().map(Item::from).toList());
    }
    public record Item(Long meetingId, String name, String coverImageUrl, String role, int participantCount,
                       int maxParticipants, String deadlineStatus, LocalDateTime deadlineAt,
                       LocalDate confirmedScheduleDate, LocalTime confirmedStartTime, String confirmedPlaceName) {
        static Item from(MyMeetingListResult.Item item) { return new Item(item.meetingId(), item.name(), item.coverImageUrl(), item.role(), item.participantCount(), item.maxParticipants(), item.deadlineStatus(), item.deadlineAt(), item.confirmedScheduleDate(), item.confirmedStartTime(), item.confirmedPlaceName()); }
    }
}
