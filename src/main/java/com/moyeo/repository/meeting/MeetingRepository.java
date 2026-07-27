package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.Meeting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    Optional<Meeting> findByInviteCode(String inviteCode);

    boolean existsByCoverImageKey(String coverImageKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Meeting r where r.id = :meetingId")
    Optional<Meeting> findByIdForUpdate(@Param("meetingId") Long meetingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Meeting r where r.inviteCode = :inviteCode")
    Optional<Meeting> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select meeting
              from Meeting meeting
             where meeting.hostUser.id = :userId
             order by meeting.id
            """)
    List<Meeting> findAllByHostUserIdForUpdate(@Param("userId") Long userId);
}
