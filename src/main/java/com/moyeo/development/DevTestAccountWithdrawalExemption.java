package com.moyeo.development;

import com.moyeo.domain.member.User;
import com.moyeo.service.member.MemberWithdrawalSocialAccountExemption;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"local", "dev"})
class DevTestAccountWithdrawalExemption implements MemberWithdrawalSocialAccountExemption {

    private final Set<Long> exemptUserIds = ConcurrentHashMap.newKeySet();

    void register(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("A persisted development test user is required.");
        }
        exemptUserIds.add(user.getId());
    }

    @Override
    public boolean appliesTo(User user) {
        return user.getId() != null && exemptUserIds.contains(user.getId());
    }
}
