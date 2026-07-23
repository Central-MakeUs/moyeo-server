package com.moyeo.repository.member;

import com.moyeo.domain.member.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    Optional<User> findFirstByNicknameAndDeletedAtIsNullOrderByIdAsc(String nickname);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User user
               set user.nickname = :nickname,
                   user.updatedAt = CURRENT_TIMESTAMP
             where user.id = :userId
               and user.nickname is null
               and user.deletedAt is null
            """)
    int completeOnboardingIfPending(
            @Param("userId") Long userId,
            @Param("nickname") String nickname
    );
}
