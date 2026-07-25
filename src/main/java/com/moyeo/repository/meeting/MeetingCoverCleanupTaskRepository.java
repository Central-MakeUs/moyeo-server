package com.moyeo.repository.meeting;

import com.moyeo.domain.meeting.MeetingCoverCleanupTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingCoverCleanupTaskRepository extends JpaRepository<MeetingCoverCleanupTask, Long> {

    List<MeetingCoverCleanupTask> findTop100ByOrderByLastAttemptedAtAscIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from MeetingCoverCleanupTask task where task.id = :taskId")
    Optional<MeetingCoverCleanupTask> findByIdForUpdate(@Param("taskId") Long taskId);
}
