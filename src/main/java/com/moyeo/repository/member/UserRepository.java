package com.moyeo.repository.member;

import com.moyeo.domain.member.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
