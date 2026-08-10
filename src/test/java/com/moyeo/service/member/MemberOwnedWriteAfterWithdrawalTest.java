package com.moyeo.service.member;

import com.moyeo.controller.TestMemberFactory;
import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.domain.place.SavedPlaceCategory;
import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.TransportationMode;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.global.security.JwtTokenProvider;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.service.meeting.CreateMeetingCommand;
import com.moyeo.service.meeting.MeetingCoverStorage;
import com.moyeo.service.meeting.MeetingService;
import com.moyeo.service.meeting.SaveParticipationCommand;
import com.moyeo.service.place.SavePlaceCommand;
import com.moyeo.service.place.SavedPlaceResult;
import com.moyeo.service.place.SavedPlaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class MemberOwnedWriteAfterWithdrawalTest {

    @Autowired
    private TestMemberFactory testMemberFactory;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private SavedPlaceService savedPlaceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private MeetingCoverStorage meetingCoverStorage;

    @Test
    void staleAuthenticatedMemberCannotCreateOwnedDataOrJoinAfterWithdrawal() {
        String accessToken = testMemberFactory.createAccessToken("withdrawn-write");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        AuthenticatedMember staleMember = new AuthenticatedMember(userId, "withdrawn-write", false);
        jdbcTemplate.update(
                "update users set nickname = null, deleted_at = current_timestamp where id = ?",
                userId
        );

        assertAuthenticationRequired(() -> meetingService.createMeeting(
                staleMember,
                new CreateMeetingCommand(
                        "blocked",
                        null,
                        2,
                        PlanningType.PLACE_ONLY,
                        ScheduleMode.NONE,
                        ScheduleInputType.NONE,
                        null,
                        null,
                        null,
                        PlaceMode.RECOMMEND,
                        null,
                        null,
                        1440
                ),
                List.of(),
                participation()
        ));
        assertAuthenticationRequired(() -> meetingService.joinMember(
                "unused-invite-code",
                staleMember,
                "blocked",
                participation()
        ));
        assertAuthenticationRequired(() -> savedPlaceService.save(
                userId,
                new SavePlaceCommand(
                        "회사",
                        SavedPlaceCategory.OTHER,
                        DeparturePlaceType.PLACE,
                        "회사",
                        "서울",
                        null,
                        null,
                        new BigDecimal("37.5"),
                        new BigDecimal("127.0")
                )
        ));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meetings where host_user_id = ?",
                Long.class,
                userId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from saved_places where user_id = ?",
                Long.class,
                userId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from meeting_participants where user_id = ?",
                Long.class,
                userId
        )).isZero();
    }

    @Test
    void staleAuthenticatedMemberCannotRenameOrDeleteSavedPlacesAfterWithdrawal() {
        String accessToken = testMemberFactory.createAccessToken("withdrawn-place-write");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        SavedPlaceResult placeToRename = savedPlaceService.save(userId, savedPlaceCommand("회사"));
        SavedPlaceResult placeToDelete = savedPlaceService.save(userId, savedPlaceCommand("집"));
        jdbcTemplate.update(
                "update users set nickname = null, deleted_at = current_timestamp where id = ?",
                userId
        );

        assertAuthenticationRequired(() ->
                savedPlaceService.rename(userId, placeToRename.id(), "변경 차단"));
        assertAuthenticationRequired(() ->
                savedPlaceService.delete(userId, placeToDelete.id()));

        assertThat(jdbcTemplate.queryForObject(
                "select alias from saved_places where id = ?",
                String.class,
                placeToRename.id()
        )).isEqualTo("회사");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from saved_places where id = ?",
                Long.class,
                placeToDelete.id()
        )).isOne();
    }

    @Test
    void savedPlaceWritesWaitForWithdrawalAndFailAfterItCommits() throws Exception {
        String accessToken = testMemberFactory.createAccessToken("withdrawal-lock");
        Long userId = jwtTokenProvider.parse(accessToken).userId();
        SavedPlaceResult placeToRename = savedPlaceService.save(userId, savedPlaceCommand("회사"));
        SavedPlaceResult placeToDelete = savedPlaceService.save(userId, savedPlaceCommand("집"));
        CountDownLatch withdrawalLockAcquired = new CountDownLatch(1);
        CountDownLatch allowWithdrawalCommit = new CountDownLatch(1);
        CountDownLatch placeWritesStarted = new CountDownLatch(2);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            CompletableFuture<Void> withdrawal = CompletableFuture.runAsync(
                    () -> holdUserLockAndWithdraw(
                            userId,
                            withdrawalLockAcquired,
                            allowWithdrawalCommit
                    ),
                    executor
            );
            assertThat(withdrawalLockAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            CompletableFuture<Throwable> renameFailure = CompletableFuture.supplyAsync(
                    () -> captureFailure(() -> {
                        placeWritesStarted.countDown();
                        savedPlaceService.rename(userId, placeToRename.id(), "변경 차단");
                    }),
                    executor
            );
            CompletableFuture<Throwable> deleteFailure = CompletableFuture.supplyAsync(
                    () -> captureFailure(() -> {
                        placeWritesStarted.countDown();
                        savedPlaceService.delete(userId, placeToDelete.id());
                    }),
                    executor
            );

            assertThat(placeWritesStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertBlockedByWithdrawalLock(renameFailure);
            assertBlockedByWithdrawalLock(deleteFailure);

            allowWithdrawalCommit.countDown();
            withdrawal.get(5, TimeUnit.SECONDS);
            assertAuthenticationRequired(renameFailure.get(5, TimeUnit.SECONDS));
            assertAuthenticationRequired(deleteFailure.get(5, TimeUnit.SECONDS));
        } finally {
            allowWithdrawalCommit.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertThat(jdbcTemplate.queryForObject(
                "select alias from saved_places where id = ?",
                String.class,
                placeToRename.id()
        )).isEqualTo("회사");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from saved_places where id = ?",
                Long.class,
                placeToDelete.id()
        )).isOne();
    }

    private void holdUserLockAndWithdraw(
            Long userId,
            CountDownLatch withdrawalLockAcquired,
            CountDownLatch allowWithdrawalCommit
    ) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = userRepository.findActiveByIdForUpdate(userId).orElseThrow();
            withdrawalLockAcquired.countDown();
            await(allowWithdrawalCommit);
            user.withdraw();
        });
    }

    private void assertBlockedByWithdrawalLock(CompletableFuture<Throwable> operation) {
        assertThatThrownBy(() -> operation.get(300, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
    }

    private Throwable captureFailure(Runnable action) {
        try {
            action.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating the withdrawal test.", exception);
        }
    }

    private SavePlaceCommand savedPlaceCommand(String alias) {
        return new SavePlaceCommand(
                alias,
                SavedPlaceCategory.OTHER,
                DeparturePlaceType.PLACE,
                alias,
                "서울",
                null,
                null,
                new BigDecimal("37.5"),
                new BigDecimal("127.0")
        );
    }

    private SaveParticipationCommand participation() {
        return new SaveParticipationCommand(
                List.of(),
                List.of(),
                new SaveParticipationCommand.Departure(
                        "회사",
                        "서울",
                        new BigDecimal("37.5"),
                        new BigDecimal("127.0"),
                        TransportationMode.PUBLIC_TRANSIT
                )
        );
    }

    private void assertAuthenticationRequired(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));
    }

    private void assertAuthenticationRequired(Throwable throwable) {
        assertThat(throwable)
                .isInstanceOfSatisfying(MoyeoException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));
    }
}
