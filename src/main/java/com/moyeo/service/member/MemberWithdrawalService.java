package com.moyeo.service.member;

import com.moyeo.domain.departure.DeparturePlaceSearch;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingCoverCleanupTask;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.domain.member.User;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.repository.departure.DeparturePlaceSearchRepository;
import com.moyeo.repository.meeting.MeetingCoverCleanupTaskRepository;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
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
    private final DeparturePlaceSearchRepository departurePlaceSearchRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingParticipantScheduleAvailabilityRepository scheduleAvailabilityRepository;
    private final MeetingParticipantScheduleDateAvailabilityRepository scheduleDateAvailabilityRepository;
    private final MeetingScheduleCandidateRepository meetingScheduleCandidateRepository;
    private final MeetingCoverCleanupTaskRepository coverCleanupTaskRepository;
    private final MeetingCoverCleanupProcessor coverCleanupProcessor;

    public MemberWithdrawalService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            SavedPlaceRepository savedPlaceRepository,
            DeparturePlaceSearchRepository departurePlaceSearchRepository,
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            MeetingParticipantScheduleAvailabilityRepository scheduleAvailabilityRepository,
            MeetingParticipantScheduleDateAvailabilityRepository scheduleDateAvailabilityRepository,
            MeetingScheduleCandidateRepository meetingScheduleCandidateRepository,
            MeetingCoverCleanupTaskRepository coverCleanupTaskRepository,
            MeetingCoverCleanupProcessor coverCleanupProcessor
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.savedPlaceRepository = savedPlaceRepository;
        this.departurePlaceSearchRepository = departurePlaceSearchRepository;
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.scheduleAvailabilityRepository = scheduleAvailabilityRepository;
        this.scheduleDateAvailabilityRepository = scheduleDateAvailabilityRepository;
        this.meetingScheduleCandidateRepository = meetingScheduleCandidateRepository;
        this.coverCleanupTaskRepository = coverCleanupTaskRepository;
        this.coverCleanupProcessor = coverCleanupProcessor;
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));
        List<Meeting> hostedMeetings = meetingRepository.findAllByHostUserIdForUpdate(userId);
        List<String> coverImageKeys = hostedMeetings.stream()
                .map(Meeting::getCoverImageKey)
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
        List<Long> coverCleanupTaskIds = createCoverCleanupTasks(coverImageKeys);

        deleteHostedMeetings(hostedMeetings);
        deleteMemberOwnedData(userId);
        user.withdraw();
        processCoverCleanupTasksAfterCommit(coverCleanupTaskIds);
    }

    private void deleteHostedMeetings(List<Meeting> hostedMeetings) {
        if (hostedMeetings.isEmpty()) {
            return;
        }

        List<Long> meetingIds = hostedMeetings.stream().map(Meeting::getId).toList();
        deleteSearchHistory(departurePlaceSearchRepository.findAllByMeetingIdIn(meetingIds));

        for (Meeting meeting : hostedMeetings) {
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
        socialAccountRepository.deleteAllByUserId(userId);
        socialAccountRepository.flush();
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
}
