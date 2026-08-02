package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingScheduleCandidateAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingScheduleCandidateAvailabilityRepository
        extends JpaRepository<MeetingScheduleCandidateAvailability, Long> {

    @Query("""
            select availability
            from MeetingScheduleCandidateAvailability availability
            join fetch availability.scheduleCandidate scheduleCandidate
            where scheduleCandidate.meeting.id = :meetingId
            order by scheduleCandidate.candidateDate, availability.startTime, availability.endTime
            """)
    List<MeetingScheduleCandidateAvailability> findAllByMeetingIdOrderByCandidateDateAndTimeAsc(
            @Param("meetingId") Long meetingId
    );

    @Modifying
    @Query("""
            delete from MeetingScheduleCandidateAvailability availability
            where availability.scheduleCandidate.meeting.id = :meetingId
            """)
    void deleteAllByMeetingId(@Param("meetingId") Long meetingId);
}
