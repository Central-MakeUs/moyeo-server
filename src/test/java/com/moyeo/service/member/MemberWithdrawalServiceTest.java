package com.moyeo.service.member;

import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.SocialAccount;
import com.moyeo.domain.member.User;
import com.moyeo.repository.feedback.FeedbackRepository;
import com.moyeo.repository.meeting.MeetingCoverCleanupTaskRepository;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingPlaceRecommendationSnapshotRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleDateAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateAvailabilityRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.place.SavedPlaceRepository;
import com.moyeo.service.meeting.MeetingCoverCleanupProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawalServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private SavedPlaceRepository savedPlaceRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;
    @Mock
    private MeetingPlaceRecommendationSnapshotRepository meetingPlaceRecommendationSnapshotRepository;
    @Mock
    private MeetingParticipantScheduleAvailabilityRepository scheduleAvailabilityRepository;
    @Mock
    private MeetingParticipantScheduleDateAvailabilityRepository scheduleDateAvailabilityRepository;
    @Mock
    private MeetingScheduleCandidateRepository meetingScheduleCandidateRepository;
    @Mock
    private MeetingScheduleCandidateAvailabilityRepository meetingScheduleCandidateAvailabilityRepository;
    @Mock
    private MeetingCoverCleanupTaskRepository coverCleanupTaskRepository;
    @Mock
    private MeetingCoverCleanupProcessor coverCleanupProcessor;
    @Mock
    private AppleLoginService appleLoginService;
    @Mock
    private KakaoLoginService kakaoLoginService;
    @Mock
    private MemberWithdrawalSocialAccountExemption exemption;

    private MemberWithdrawalService memberWithdrawalService;

    @BeforeEach
    void setUp() {
        memberWithdrawalService = new MemberWithdrawalService(
                userRepository,
                socialAccountRepository,
                savedPlaceRepository,
                feedbackRepository,
                meetingRepository,
                meetingParticipantRepository,
                meetingPlaceRecommendationSnapshotRepository,
                scheduleAvailabilityRepository,
                scheduleDateAvailabilityRepository,
                meetingScheduleCandidateRepository,
                meetingScheduleCandidateAvailabilityRepository,
                coverCleanupTaskRepository,
                coverCleanupProcessor,
                appleLoginService,
                kakaoLoginService,
                List.of(exemption)
        );
    }

    @Test
    void disconnectsProviderOnlyAfterLocalChangesAreFlushed() {
        User user = activeUser();
        when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findAllByUserId(1L))
                .thenReturn(List.of(new SocialAccount(
                        user,
                        AuthProvider.APPLE,
                        "apple-subject",
                        null,
                        "encrypted-refresh-token"
                )));
        when(meetingRepository.findAllByHostUserIdForUpdate(1L)).thenReturn(List.of());

        memberWithdrawalService.withdraw(1L);

        InOrder order = inOrder(userRepository, appleLoginService);
        order.verify(userRepository).flush();
        order.verify(appleLoginService).disconnectStoredAuthorization(
                "apple-subject",
                "encrypted-refresh-token"
        );
    }

    @Test
    void localFlushFailurePreventsProviderDisconnect() {
        User user = activeUser();
        when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findAllByUserId(1L))
                .thenReturn(List.of(new SocialAccount(user, AuthProvider.KAKAO, "1234567890", null)));
        when(meetingRepository.findAllByHostUserIdForUpdate(1L)).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("flush failed"))
                .when(userRepository)
                .flush();

        assertThatThrownBy(() -> memberWithdrawalService.withdraw(1L))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(kakaoLoginService, never())
                .disconnectStoredAccount("1234567890");
    }

    @Test
    void missingSocialAccountIsRejectedWithoutExplicitDevelopmentExemption() {
        User user = activeUser();
        when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findAllByUserId(1L)).thenReturn(List.of());
        when(exemption.appliesTo(user)).thenReturn(false);

        assertThatThrownBy(() -> memberWithdrawalService.withdraw(1L))
                .isInstanceOf(IllegalStateException.class);

        verify(meetingRepository, never()).findAllByHostUserIdForUpdate(1L);
    }

    @Test
    void explicitDevelopmentExemptionAllowsWithdrawalWithoutSocialAccount() {
        User user = activeUser();
        when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findAllByUserId(1L)).thenReturn(List.of());
        when(exemption.appliesTo(user)).thenReturn(true);
        when(meetingRepository.findAllByHostUserIdForUpdate(1L)).thenReturn(List.of());

        memberWithdrawalService.withdraw(1L);

        verifyNoInteractions(appleLoginService, kakaoLoginService);
        verify(userRepository).flush();
    }

    @Test
    void withdrawalDeletesMembersFeedbackBeforeSocialAccountLink() {
        User user = activeUser();
        when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findAllByUserId(1L)).thenReturn(List.of());
        when(exemption.appliesTo(user)).thenReturn(true);
        when(meetingRepository.findAllByHostUserIdForUpdate(1L)).thenReturn(List.of());

        memberWithdrawalService.withdraw(1L);

        InOrder order = inOrder(feedbackRepository, socialAccountRepository);
        order.verify(feedbackRepository).deleteAllByUserId(1L);
        order.verify(feedbackRepository).flush();
        order.verify(socialAccountRepository).deleteAllByUserId(1L);
    }

    @Test
    void missingStoredAppleRefreshTokenPreventsLocalWithdrawal() {
        User user = activeUser();
        when(userRepository.findActiveByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(socialAccountRepository.findAllByUserId(1L))
                .thenReturn(List.of(new SocialAccount(user, AuthProvider.APPLE, "apple-subject", null)));

        assertThatThrownBy(() -> memberWithdrawalService.withdraw(1L))
                .isInstanceOfSatisfying(com.moyeo.global.error.MoyeoException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(com.moyeo.global.security.AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE)
                );

        verify(meetingRepository, never()).findAllByHostUserIdForUpdate(1L);
        verifyNoInteractions(appleLoginService, kakaoLoginService);
    }

    private User activeUser() {
        User user = new User("일반 사용자");
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
