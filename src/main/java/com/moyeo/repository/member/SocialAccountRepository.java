package com.moyeo.repository.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    List<SocialAccount> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
