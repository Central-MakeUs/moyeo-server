package com.moyeo.service.member;

import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.member.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private UserRepository userRepository;

    @Test
    void refreshRotatesTokenAndRejectsThePreviousToken() {
        Long userId = createUser();
        RefreshTokenService.SessionToken original = refreshTokenService.issue(userId);

        RefreshTokenService.RefreshSession session = refreshTokenService.refresh(original.refreshToken());

        assertThat(session.member().userId()).isEqualTo(userId);
        assertThat(session.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(session.sessionId()).isEqualTo(original.sessionId());
        assertThatThrownBy(() -> refreshTokenService.refresh(original.refreshToken()))
                .isInstanceOf(MoyeoException.class);
        assertThat(refreshTokenService.refresh(session.refreshToken()).member().userId()).isEqualTo(userId);
    }

    @Test
    void logoutInvalidatesOnlyThePresentedToken() {
        Long userId = createUser();
        RefreshTokenService.SessionToken currentDevice = refreshTokenService.issue(userId);
        RefreshTokenService.SessionToken otherDevice = refreshTokenService.issue(userId);

        refreshTokenService.logout(currentDevice.refreshToken());

        assertThat(refreshTokenService.isActiveSession(userId, currentDevice.sessionId())).isFalse();
        assertThat(refreshTokenService.isActiveSession(userId, otherDevice.sessionId())).isTrue();
        assertThatThrownBy(() -> refreshTokenService.refresh(currentDevice.refreshToken()))
                .isInstanceOf(MoyeoException.class);
        assertThat(refreshTokenService.refresh(otherDevice.refreshToken()).member().userId()).isEqualTo(userId);
    }

    @Test
    void logoutRejectsAnUnknownToken() {
        assertThatThrownBy(() -> refreshTokenService.logout("unknown-refresh-token"))
                .isInstanceOf(MoyeoException.class);
    }

    @Test
    void deletingAllTokensInvalidatesEveryDeviceSession() {
        Long userId = createUser();
        RefreshTokenService.SessionToken first = refreshTokenService.issue(userId);
        RefreshTokenService.SessionToken second = refreshTokenService.issue(userId);

        refreshTokenService.deleteAllByUserId(userId);

        assertThat(refreshTokenService.isActiveSession(userId, first.sessionId())).isFalse();
        assertThat(refreshTokenService.isActiveSession(userId, second.sessionId())).isFalse();
        assertThatThrownBy(() -> refreshTokenService.refresh(first.refreshToken())).isInstanceOf(MoyeoException.class);
        assertThatThrownBy(() -> refreshTokenService.refresh(second.refreshToken())).isInstanceOf(MoyeoException.class);
    }

    private Long createUser() {
        return userRepository.save(new User("테스트사용자")).getId();
    }
}
