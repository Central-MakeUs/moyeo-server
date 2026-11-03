package com.moyeo.repository.member;

import com.moyeo.domain.member.AppleRevokeTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppleRevokeTaskRepository extends JpaRepository<AppleRevokeTask, Long> {
    boolean existsByProviderUserId(String providerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from AppleRevokeTask task where task.id = :id")
    Optional<AppleRevokeTask> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from AppleRevokeTask task where task.providerUserId = :providerUserId")
    Optional<AppleRevokeTask> findByProviderUserIdForUpdate(@Param("providerUserId") String providerUserId);

    @Query("select task.id from AppleRevokeTask task where task.nextAttemptAt <= :now order by task.nextAttemptAt, task.id")
    List<Long> findDueIds(@Param("now") LocalDateTime now, Pageable pageable);
}
