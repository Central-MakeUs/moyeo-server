package com.moyeo.controller.member;

import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.controller.TestMemberFactory;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.KakaoUnlinkTask;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.repository.member.KakaoUnlinkTaskRepository;
import com.moyeo.service.member.KakaoUnlinkProcessor;
import com.moyeo.service.member.MemberAuthService;
import com.moyeo.service.member.SocialAccountRegistrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "moyeo.kakao-unlink.scan-delay=24h")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class KakaoWithdrawalRecoveryTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private TestMemberFactory members;
    @Autowired private JwtTokenProvider jwt;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private KakaoUnlinkProcessor processor;
    @Autowired private MemberAuthService auth;
    @Autowired private SocialAccountRegistrationService registration;
    @MockitoSpyBean private KakaoUnlinkTaskRepository tasks;
    @MockitoBean private KakaoLoginService kakao;
    @MockitoBean private AppleLoginService apple;

    @AfterEach
    void cleanup() {
        tasks.deleteAll();
    }

    @Test
    void failureCommitsLocalWithdrawalAndGatesSignupUntilScheduledRetrySucceeds() throws Exception {
        String token = account("recovery-failure", "recovery-failure-id");
        Long oldUserId = jwt.parse(token).userId();
        doThrow(new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE))
                .when(kakao).disconnectStoredAccount("recovery-failure-id");

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());

        assertThat(jdbc.queryForObject("select deleted_at is not null from users where id = ?", Boolean.class, oldUserId)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from social_accounts where user_id = ?", Long.class, oldUserId)).isZero();
        assertThat(tasks.findAll()).singleElement().satisfies(task -> assertThat(task.getAttemptCount()).isEqualTo(1));
        verify(kakao, times(1)).disconnectStoredAccount("recovery-failure-id");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        assertPending(() -> auth.loginSocial(AuthProvider.KAKAO, "recovery-failure-id"));
        when(kakao.loginWithAccessToken("pending-token"))
                .thenAnswer(invocation -> auth.loginKakao("recovery-failure-id", () -> {}));
        mockMvc.perform(post("/api/auth/kakao/native").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"pending-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOCIAL_DISCONNECTION_PENDING"));
        Long before = jdbc.queryForObject("select count(*) from users", Long.class);
        assertPending(() -> registration.register(AuthProvider.KAKAO, "recovery-failure-id"));
        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isEqualTo(before);

        processor.processPending();
        verify(kakao, times(1)).disconnectStoredAccount("recovery-failure-id");
        doNothing().when(kakao).disconnectStoredAccount("recovery-failure-id");
        jdbc.update("update kakao_unlink_tasks set next_attempt_at = dateadd('MINUTE', -1, current_timestamp)");
        processor.processPending();

        verify(kakao, times(2)).disconnectStoredAccount("recovery-failure-id");
        assertThat(tasks.count()).isZero();
        assertThat(auth.loginSocial(AuthProvider.KAKAO, "recovery-failure-id").userId()).isNotEqualTo(oldUserId);
    }

    @Test
    void immediateSuccessRunsAfterLocalCommitAndReturns204() throws Exception {
        String token = account("recovery-success", "recovery-success-id");
        Long userId = jwt.parse(token).userId();
        doAnswer(invocation -> {
            assertThat(jdbc.queryForObject("select deleted_at is not null from users where id = ?", Boolean.class, userId)).isTrue();
            assertThat(tasks.count()).isEqualTo(1);
            return null;
        }).when(kakao).disconnectStoredAccount("recovery-success-id");

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertThat(tasks.count()).isZero();
        verify(kakao, times(1)).disconnectStoredAccount("recovery-success-id");
    }

    @Test
    void failureToStoreTaskRollsBackWithdrawalWithoutCallingKakao() throws Exception {
        String token = account("recovery-store-failure", "recovery-store-failure-id");
        Long userId = jwt.parse(token).userId();
        doThrow(new DataAccessResourceFailureException("storage unavailable")).when(tasks).saveAndFlush(any(KakaoUnlinkTask.class));

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
        assertThat(jdbc.queryForObject("select deleted_at is null from users where id = ?", Boolean.class, userId)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from social_accounts where user_id = ?", Long.class, userId)).isEqualTo(1);
        verify(kakao, never()).disconnectStoredAccount("recovery-store-failure-id");
    }

    @Test
    void failureToConfirmExternalCompletionRetainsTaskForRetry() throws Exception {
        String token = account("recovery-confirm-failure", "recovery-confirm-failure-id");
        doThrow(new DataAccessResourceFailureException("flush failed")).doNothing().when(tasks).flush();

        mockMvc.perform(delete("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());
        assertThat(tasks.count()).isEqualTo(1);

        processor.processPending();
        assertThat(tasks.count()).isZero();
        verify(kakao, times(2)).disconnectStoredAccount("recovery-confirm-failure-id");
    }

    @Test
    void concurrentWorkersDoNotSendDuplicateUnlinkRequests() throws Exception {
        KakaoUnlinkTask task = tasks.saveAndFlush(new KakaoUnlinkTask(99999L, "recovery-concurrent-id"));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(kakao).disconnectStoredAccount("recovery-concurrent-id");

        try (var workers = Executors.newFixedThreadPool(2)) {
            var first = workers.submit(() -> processor.process(task.getId()));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            var second = workers.submit(() -> {
                secondStarted.countDown();
                return processor.process(task.getId());
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
        }
        verify(kakao, times(1)).disconnectStoredAccount("recovery-concurrent-id");
    }

    @Test
    void staleTaskCannotDisconnectAnActiveMembership() {
        String token = account("recovery-stale", "recovery-stale-id");
        KakaoUnlinkTask task = tasks.saveAndFlush(new KakaoUnlinkTask(jwt.parse(token).userId(), "recovery-stale-id"));
        assertThat(processor.process(task.getId())).isFalse();
        verify(kakao, never()).disconnectStoredAccount("recovery-stale-id");
        assertThat(tasks.count()).isEqualTo(1);
    }

    private void assertPending(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(MoyeoException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthenticationErrorCode.SOCIAL_DISCONNECTION_PENDING));
    }

    private String account(String nickname, String providerId) {
        String token = members.createAccessToken(nickname);
        jdbc.update("insert into social_accounts (user_id, provider, provider_user_id, created_at) values (?, 'KAKAO', ?, current_timestamp)",
                jwt.parse(token).userId(), providerId);
        return token;
    }
}
