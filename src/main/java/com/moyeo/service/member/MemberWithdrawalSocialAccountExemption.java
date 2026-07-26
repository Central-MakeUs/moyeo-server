package com.moyeo.service.member;

import com.moyeo.domain.member.User;

public interface MemberWithdrawalSocialAccountExemption {

    boolean appliesTo(User user);
}
