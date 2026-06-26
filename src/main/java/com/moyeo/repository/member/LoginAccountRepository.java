package com.moyeo.repository.member;

import com.moyeo.domain.member.LoginAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginAccountRepository extends JpaRepository<LoginAccount, Long> {

    boolean existsByLoginId(String loginId);

    Optional<LoginAccount> findByLoginId(String loginId);
}
