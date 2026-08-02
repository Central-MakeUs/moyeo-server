package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingPlaceRecommendationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeetingPlaceRecommendationSnapshotRepository extends JpaRepository<MeetingPlaceRecommendationSnapshot, Long> {
    List<MeetingPlaceRecommendationSnapshot> findAllByMeetingIdOrderByRankAsc(Long meetingId);
}
