package com.moyeo.service.member;

import com.moyeo.auth.apple.AppleLoginService;
import com.moyeo.auth.kakao.KakaoLoginService;
import com.moyeo.domain.departure.DeparturePlaceSearch;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingCoverCleanupTask;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.domain.member.AuthProvider;
import com.moyeo.domain.member.SocialAccount;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.departure.DeparturePlaceSearchRepository;
import com.moyeo.repository.feedback.FeedbackRepository;
import com.moyeo.repository.meeting.MeetingCoverCleanupTaskRepository;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingPlaceRecommendationSnapshotRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleDateAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateRepository;
import com.moyeo.repository.member.SocialAccountRepository;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.place.SavedPlaceRepository;
import com.moyeo.service.meeting.MeetingCoverCleanupProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class MemberWithdrawalService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final SavedPlaceRepository savedPlaceRepository;
    private final FeedbackRepository feedbackRepository;
    private final DeparturePlaceSearchRepository departurePlaceSearchRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingPlaceRecommendationSnapshotRepository meetingPlaceRecommendationSnapshotRepository;
    private final MeetingParticipantScheduleAvailabilityRepository scheduleAvailabilityRepository;
    private final MeetingParticipantScheduleDateAvailabilityRepository scheduleDateAvailabilityRepository;
    private final MeetingScheduleCandidateRepository meetingScheduleCandidateRepository;
    private final MeetingCoverCleanupTaskRepository coverCleanupTaskRepository;
    private final MeetingCoverCleanupProcessor coverCleanupProcessor;
    private final AppleLoginService appleLoginService;
    private final KakaoLoginService kakaoLoginService;
    private final List<MemberWithdrawalSocialAccountExemption> socialAccountExemptions;

    public MemberWithdrawalService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            SavedPlaceRepository savedPlaceRepository,
            FeedbackRepository feedbackRepository,
            DeparturePlaceSearchRepository departurePlaceSearchRepository,
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            MeetingPlaceRecommendationSnapshotRepository meetingPlaceRecommendationSnapshotRepository,
            MeetingParticipantScheduleAvailabilityRepository scheduleAvailabilityRepository,
            MeetingParticipantScheduleDateAvailabilityRepository scheduleDateAvailabilityRepository,
            MeetingScheduleCandidateRepository meetingScheduleCandidateRepository,
            MeetingCoverCleanupTaskRepository coverCleanupTaskRepository,
            MeetingCoverCleanupProcessor coverCleanupProcessor,
            AppleLoginService appleLoginService,
            KakaoLoginService kakaoLoginService,
            List<MemberWithdrawalSocialAccountExemption> socialAccountExemptions
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.savedPlaceRepository = savedPlaceRepository;
        this.feedbackRepository = feedbackRepository;
        this.departurePlaceSearchRepository = departurePlaceSearchRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingPlaceRecommendationSnapshotRepository = meetingPlaceRecommendationSnapshotRepository;
        this.scheduleAvailabilityRepository = scheduleAvailabilityRepository;
        this.scheduleDateAvailabilityRepository = scheduleDateAvailabilityRepository;
        this.meetingScheduleCandidateRepository = meetingScheduleCandidateRepository;
        this.coverCleanupTaskRepository = coverCleanupTaskRepository;
        this.coverCleanupProcessor = coverCleanupProcessor;
        this.appleLoginService = appleLoginService;
        this.kakaoLoginService = kakaoLoginService;
        this.socialAccountExemptions = socialAccountExemptions;
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));
        SocialDisconnectTarget disconnectTarget = resolveDisconnectTarget(user);
        List<Meeting> hostedMeetings = meetingRepository.findAllByHostUserIdForUpdate(userId);
        List<String> coverImageKeys = hostedMeetings.stream()
                .map(Meeting::getCoverImageKey)
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
        List<Long> coverCleanupTaskIds = createCoverCleanupTasks(coverImageKeys);

        deleteHostedMeetings(hostedMeetings);
        deleteMemberParticipations(userId);
        deleteMemberOwnedData(userId);
        user.withdraw();
        userRepository.flush();
        disconnectSocialAccount(disconnectTarget);
        processCoverCleanupTasksAfterCommit(coverCleanupTaskIds);
    }

    private SocialDisconnectTarget resolveDisconnectTarget(User user) {
        List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUserId(user.getId());
        if (socialAccounts.isEmpty()) {
            if (isSocialAccountExempt(user)) {
                return null;
            }
            throw new IllegalStateException("Active non-development user must have one social account.");
        }
        if (socialAccounts.size() != 1) {
            throw new IllegalStateException("Active user must have exactly one social account.");
        }

        SocialAccount socialAccount = socialAccounts.getFirst();
        if (socialAccount.getProvider() == AuthProvider.APPLE
                && (socialAccount.getProviderRefreshTokenCiphertext() == null
                || socialAccount.getProviderRefreshTokenCiphertext().isBlank())) {
            throw new MoyeoException(AuthenticationErrorCode.SOCIAL_LOGIN_UNAVAILABLE);
        }
        return new SocialDisconnectTarget(
                socialAccount.getProvider(),
                socialAccount.getProviderUserId(),
                socialAccount.getProviderRefreshTokenCiphertext()
        );
    }

    private boolean isSocialAccountExempt(User user) {
        return socialAccountExemptions.stream()
                .anyMatch(exemption -> exemption.appliesTo(user));
    }

    private void disconnectSocialAccount(SocialDisconnectTarget target) {
        if (target == null) {
            return;
        }
        if (target.provider() == AuthProvider.APPLE) {
            appleLoginService.disconnectStoredAuthorization(
                    target.providerUserId(),
                    target.providerRefreshTokenCiphertext()
            );
            return;
        }
        kakaoLoginService.disconnectStoredAccount(target.providerUserId());
    }

    private void deleteHostedMeetings(List<Meeting> hostedMeetings) {
        if (hostedMeetings.isEmpty()) {
            return;
        }

        List<Long> meetingIds = hostedMeetings.stream().map(Meeting::getId).toList();
        deleteSearchHistory(departurePlaceSearchRepository.findAllByMeetingIdIn(meetingIds));

        for (Meeting meeting : hostedMeetings) {
            deletePlaceRecommendationSnapshots(meeting.getId());
            List<MeetingParticipant> participants =
                    meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId());
            for (MeetingParticipant participant : participants) {
                scheduleDateAvailabilityRepository.deleteAllByParticipantId(participant.getId());
                scheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());
            }
            scheduleDateAvailabilityRepository.flush();
            scheduleAvailabilityRepository.flush();

            meetingParticipantRepository.deleteAll(participants);
            meetingParticipantRepository.flush();
            meetingScheduleCandidateRepository.deleteAllByMeetingId(meeting.getId());
            meetingScheduleCandidateRepository.flush();
        }

        meetingRepository.deleteAll(hostedMeetings);
        meetingRepository.flush();
    }

    private void deleteMemberOwnedData(Long userId) {
        deleteSearchHistory(departurePlaceSearchRepository.findAllByUserId(userId));
        savedPlaceRepository.deleteAllByUserId(userId);
        savedPlaceRepository.flush();
        feedbackRepository.deleteAllByUserId(userId);
        feedbackRepository.flush();
        socialAccountRepository.deleteAllByUserId(userId);
        socialAccountRepository.flush();
    }

    private void deleteMemberParticipations(Long userId) {
        List<MeetingParticipant> participations = meetingParticipantRepository.findAllByUserIdWithMeeting(userId);
        for (MeetingParticipant participant : participations) {
            Meeting meeting = meetingRepository.findByIdForUpdate(participant.getMeeting().getId())
                    .orElseThrow(() -> new IllegalStateException("Meeting disappeared during member withdrawal."));
            removeMemberParticipant(meeting, participant);
        }
    }

    private void removeMemberParticipant(Meeting meeting, MeetingParticipant participant) {
        if (meetingParticipantRepository.countByMeetingId(meeting.getId()) == meeting.getMaxParticipants()) {
            deletePlaceRecommendationSnapshots(meeting.getId());
        }
        scheduleDateAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        scheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        scheduleDateAvailabilityRepository.flush();
        scheduleAvailabilityRepository.flush();
        meetingParticipantRepository.delete(participant);
        meetingParticipantRepository.flush();
        meeting.decreaseMaxParticipants();
    }

    private void deletePlaceRecommendationSnapshots(Long meetingId) {
        meetingPlaceRecommendationSnapshotRepository.deleteAllByMeetingId(meetingId);
        meetingPlaceRecommendationSnapshotRepository.flush();
    }

    private void deleteSearchHistory(List<DeparturePlaceSearch> searches) {
        if (searches.isEmpty()) {
            return;
        }
        departurePlaceSearchRepository.deleteAll(searches);
        departurePlaceSearchRepository.flush();
    }

    private List<Long> createCoverCleanupTasks(List<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return List.of();
        }
        List<MeetingCoverCleanupTask> tasks = objectKeys.stream()
                .map(MeetingCoverCleanupTask::new)
                .toList();
        coverCleanupTaskRepository.saveAllAndFlush(tasks);
        return tasks.stream().map(MeetingCoverCleanupTask::getId).toList();
    }

    private void processCoverCleanupTasksAfterCommit(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                coverCleanupProcessor.process(taskIds);
            }
        });
    }

    private record SocialDisconnectTarget(
            AuthProvider provider,
            String providerUserId,
            String providerRefreshTokenCiphertext
    ) {
    }
}
