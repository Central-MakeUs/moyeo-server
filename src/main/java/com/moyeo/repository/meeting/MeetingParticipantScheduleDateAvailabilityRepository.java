package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingParticipantScheduleDateAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingParticipantScheduleDateAvailabilityRepository
        extends JpaRepository<MeetingParticipantScheduleDateAvailability, Long> {

    long countByParticipantId(Long participantId);

    @Query("""
            select availability
            from MeetingParticipantScheduleDateAvailability availability
            join fetch availability.participant participant
            join fetch availability.scheduleCandidate scheduleCandidate
            where participant.meeting.id = :meetingId
            """)
    List<MeetingParticipantScheduleDateAvailability> findAllByParticipantMeetingId(@Param("meetingId") Long meetingId);

    @Query("""
            select availability
            from MeetingParticipantScheduleDateAvailability availability
            join fetch availability.scheduleCandidate scheduleCandidate
            where availability.participant.id = :participantId
            order by scheduleCandidate.candidateDate
            """)
    List<MeetingParticipantScheduleDateAvailability> findAllByParticipantIdOrderByCandidateDateAsc(
            @Param("participantId") Long participantId
    );

    void deleteAllByParticipantId(Long participantId);
}
