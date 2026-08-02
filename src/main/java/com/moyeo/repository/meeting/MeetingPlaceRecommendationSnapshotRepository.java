package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingPlaceRecommendationSnapshot;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MeetingPlaceRecommendationSnapshotRepository extends JpaRepository<MeetingPlaceRecommendationSnapshot, Long> {
    List<MeetingPlaceRecommendationSnapshot> findAllByMeetingIdOrderByRankAsc(Long meetingId);

    @Modifying
    @Query("delete from MeetingPlaceRecommendationSnapshot snapshot where snapshot.meeting.id = :meetingId")
    void deleteAllByMeetingId(@Param("meetingId") Long meetingId);
}
