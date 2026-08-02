package com.moyeo.service.member;

import com.moyeo.domain.member.User;
import com.moyeo.domain.member.ProfileColor;

public record AuthenticatedMember(
        Long userId,
        String nickname,
        ProfileColor profileColor,
        boolean registered
) {

    public AuthenticatedMember(Long userId, String nickname, boolean registered) {
        this(userId, nickname, ProfileColor.GRAY, registered);
    }

    public static AuthenticatedMember from(User user, boolean registered) {
        return new AuthenticatedMember(user.getId(), user.getNickname(), user.getProfileColor(), registered);
    }

    public boolean onboardingCompleted() {
        return nickname != null;
    }
}
