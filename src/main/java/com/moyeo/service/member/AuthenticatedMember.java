package com.moyeo.service.member;

import com.moyeo.domain.member.User;

public record AuthenticatedMember(
        Long userId,
        String nickname,
        boolean registered
) {

    public static AuthenticatedMember from(User user, boolean registered) {
        return new AuthenticatedMember(user.getId(), user.getNickname(), registered);
    }
}
