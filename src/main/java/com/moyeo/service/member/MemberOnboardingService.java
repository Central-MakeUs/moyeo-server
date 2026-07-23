package com.moyeo.service.member;

import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberOnboardingService {

    private final UserRepository userRepository;

    public MemberOnboardingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AuthenticatedMember complete(Long userId, String nickname) {
        int updated = userRepository.completeOnboardingIfPending(userId, nickname);
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));

        if (updated == 0 && !nickname.equals(user.getNickname())) {
            throw new MoyeoException(AuthenticationErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }
        return AuthenticatedMember.from(user, false);
    }
}
