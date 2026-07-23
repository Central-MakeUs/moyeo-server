package com.moyeo.service.member;

import com.moyeo.domain.member.AuthProvider;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.member.SocialAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({MemberAuthService.class, MemberOnboardingService.class})
class MemberAuthServiceTest {

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberOnboardingService memberOnboardingService;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void socialLoginCreatesPendingUserOnFirstLogin() {
        AuthenticatedMember member = memberAuthService.loginSocial(AuthProvider.APPLE, "apple-123");

        assertThat(member.userId()).isNotNull();
        assertThat(member.nickname()).isNull();
        assertThat(member.onboardingCompleted()).isFalse();
        assertThat(member.registered()).isTrue();
        assertThat(socialAccountRepository.findByProviderAndProviderUserId(AuthProvider.APPLE, "apple-123"))
                .get()
                .extracting(account -> account.getEmail())
                .isNull();
    }

    @Test
    void socialLoginReturnsSameUserForSameProviderSubject() {
        AuthenticatedMember registered = memberAuthService.loginSocial(AuthProvider.APPLE, "apple-123");
        AuthenticatedMember loggedIn = memberAuthService.loginSocial(AuthProvider.APPLE, "apple-123");

        assertThat(loggedIn.userId()).isEqualTo(registered.userId());
        assertThat(loggedIn.registered()).isFalse();
    }

    @Test
    void differentProvidersCreateSeparateUsersWithoutEmailMerge() {
        AuthenticatedMember apple = memberAuthService.loginSocial(AuthProvider.APPLE, "same-person");
        AuthenticatedMember kakao = memberAuthService.loginSocial(AuthProvider.KAKAO, "same-person");

        assertThat(kakao.userId()).isNotEqualTo(apple.userId());
    }

    @Test
    void socialLoginRejectsSoftDeletedUser() {
        AuthenticatedMember registered = memberAuthService.loginSocial(AuthProvider.APPLE, "deleted-apple");
        softDeleteUser(registered.userId());

        assertThatThrownBy(() -> memberAuthService.loginSocial(AuthProvider.APPLE, "deleted-apple"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_LOGIN_FAILED)
                );
    }

    @Test
    void onboardingIsIdempotentForSameNickname() {
        AuthenticatedMember registered = memberAuthService.loginSocial(AuthProvider.APPLE, "onboarding-apple");

        AuthenticatedMember completed = memberOnboardingService.complete(registered.userId(), "모여");
        AuthenticatedMember retried = memberOnboardingService.complete(registered.userId(), "모여");

        assertThat(completed.nickname()).isEqualTo("모여");
        assertThat(retried.nickname()).isEqualTo("모여");
        assertThat(retried.onboardingCompleted()).isTrue();
    }

    @Test
    void onboardingRejectsDifferentNicknameAfterCompletion() {
        AuthenticatedMember registered = memberAuthService.loginSocial(AuthProvider.APPLE, "completed-apple");
        memberOnboardingService.complete(registered.userId(), "모여");

        assertThatThrownBy(() -> memberOnboardingService.complete(registered.userId(), "다른 이름"))
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AuthenticationErrorCode.ONBOARDING_ALREADY_COMPLETED)
                );
    }

    private void softDeleteUser(Long userId) {
        jdbcTemplate.update("update users set deleted_at = current_timestamp where id = ?", userId);
        entityManager.clear();
    }
}
