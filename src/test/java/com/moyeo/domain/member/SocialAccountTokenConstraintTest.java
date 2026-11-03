package com.moyeo.domain.member;

import com.moyeo.repository.member.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class SocialAccountTokenConstraintTest {
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void databaseRejectsAppleInsertWithoutTokenEvenWhenJpaIsBypassed(String token) {
        Long userId = users.saveAndFlush(User.pendingOnboarding()).getId();
        assertThatThrownBy(() -> insert(userId, "APPLE", token))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(exception -> assertThat(exception.getMessage())
                        .containsIgnoringCase("ck_social_accounts_apple_refresh_token"));
        assertThat(count(userId)).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void databaseRejectsClearingAppleTokenAndPreservesExistingValue(String token) {
        Long userId = users.saveAndFlush(User.pendingOnboarding()).getId();
        insert(userId, "APPLE", "encrypted-original");
        assertThatThrownBy(() -> jdbc.update(
                "update social_accounts set provider_refresh_token_ciphertext = ? where user_id = ?", token, userId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(exception -> assertThat(exception.getMessage())
                        .containsIgnoringCase("ck_social_accounts_apple_refresh_token"));
        assertThat(jdbc.queryForObject("select provider_refresh_token_ciphertext from social_accounts where user_id = ?",
                String.class, userId)).isEqualTo("encrypted-original");
    }

    @Test
    void databaseAllowsAppleTokenRotationAndKakaoNullToken() {
        Long appleId = users.saveAndFlush(User.pendingOnboarding()).getId();
        Long kakaoId = users.saveAndFlush(User.pendingOnboarding()).getId();
        insert(appleId, "APPLE", "encrypted-original");
        insert(kakaoId, "KAKAO", null);
        jdbc.update("update social_accounts set provider_refresh_token_ciphertext = ? where user_id = ?",
                "encrypted-rotated", appleId);
        assertThat(count(appleId)).isEqualTo(1);
        assertThat(count(kakaoId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select provider_refresh_token_ciphertext from social_accounts where user_id = ?",
                String.class, kakaoId)).isNull();
    }

    private void insert(Long userId, String provider, String token) {
        jdbc.update("insert into social_accounts(user_id, provider, provider_user_id, provider_refresh_token_ciphertext, created_at) "
                + "values (?, ?, ?, ?, current_timestamp)", userId, provider, "constraint-" + userId, token);
    }

    private Long count(Long userId) {
        return jdbc.queryForObject("select count(*) from social_accounts where user_id = ?", Long.class, userId);
    }
}
