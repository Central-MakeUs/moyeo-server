package com.moyeo.controller;

import com.moyeo.domain.member.User;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestMemberFactory {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public TestMemberFactory(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public String createAccessToken(String nickname) {
        User user = userRepository.save(new User(nickname));
        return jwtTokenProvider.createAccessToken(AuthenticatedMember.from(user, false));
    }

    @Transactional
    public String createPendingAccessToken() {
        User user = userRepository.save(User.pendingOnboarding());
        return jwtTokenProvider.createAccessToken(AuthenticatedMember.from(user, false));
    }
}
