package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.ParticipantType;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {

    long countByMeetingId(Long meetingId);

    boolean existsByMeetingAndNicknameAndParticipantType(Meeting meeting, String nickname, ParticipantType participantType);

    boolean existsByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("""
            select participant
              from MeetingParticipant participant
              left join fetch participant.user
             where participant.meeting.id = :meetingId
             order by participant.id
            """)
    List<MeetingParticipant> findAllByMeetingIdOrderByIdAsc(@Param("meetingId") Long meetingId);

    Optional<MeetingParticipant> findByIdAndMeetingId(Long id, Long meetingId);

    Optional<MeetingParticipant> findByMeetingIdAndUserId(Long meetingId, Long userId);

    @Query("""
            select participant from MeetingParticipant participant
            join fetch participant.meeting meeting
            join fetch meeting.hostUser
            where participant.user.id = :userId
            """)
    List<MeetingParticipant> findAllByUserIdWithMeeting(@Param("userId") Long userId);
}
