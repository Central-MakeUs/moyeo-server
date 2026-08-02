package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingParticipantScheduleAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingParticipantScheduleAvailabilityRepository extends JpaRepository<MeetingParticipantScheduleAvailability, Long> {

    long countByParticipantId(Long participantId);

    @Query("""
            select availability
            from MeetingParticipantScheduleAvailability availability
            join fetch availability.participant participant
            join fetch availability.scheduleCandidate scheduleCandidate
            where participant.meeting.id = :meetingId
            """)
    List<MeetingParticipantScheduleAvailability> findAllByParticipantMeetingId(@Param("meetingId") Long meetingId);

    @Query("""
            select availability
            from MeetingParticipantScheduleAvailability availability
            join fetch availability.scheduleCandidate scheduleCandidate
            where availability.participant.id = :participantId
            order by scheduleCandidate.candidateDate, availability.startTime, availability.endTime
            """)
    List<MeetingParticipantScheduleAvailability> findAllByParticipantIdOrderByCandidateDateAndTimeAsc(
            @Param("participantId") Long participantId
    );

    void deleteAllByParticipantId(Long participantId);
}
