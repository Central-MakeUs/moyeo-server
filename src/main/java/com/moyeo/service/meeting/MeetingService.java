package com.moyeo.service.meeting;

import com.moyeo.domain.member.User;
import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaStationLineEntity;
import com.moyeo.domain.departure.DepartureRegionPolicy;
import com.moyeo.domain.meeting.ParticipantType;
import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlaceRecommendationStrategy;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.domain.meeting.MeetingParticipantScheduleDateAvailability;
import com.moyeo.domain.meeting.MeetingParticipantScheduleAvailability;
import com.moyeo.domain.meeting.MeetingScheduleCandidateAvailability;
import com.moyeo.domain.meeting.MeetingScheduleCandidate;
import com.moyeo.domain.meeting.MeetingPlaceRecommendationSnapshot;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.route.KakaoRouteProperties;
import com.moyeo.route.KakaoRouteClient;
import com.moyeo.route.KakaoRouteUnavailableException;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.commercial.CommercialAreaStationLineRepository;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleDateAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingPlaceRecommendationSnapshotRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingParticipantScheduleDateAvailabilityRepository meetingParticipantScheduleDateAvailabilityRepository;
    private final MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository;
    private final MeetingScheduleCandidateRepository meetingScheduleCandidateRepository;
    private final MeetingScheduleCandidateAvailabilityRepository meetingScheduleCandidateAvailabilityRepository;
    private final MeetingPlaceRecommendationSnapshotRepository meetingPlaceRecommendationSnapshotRepository;
    private final UserRepository userRepository;
    private final CommercialAreaCatalog commercialAreaCatalog;
    private final CommercialAreaStationLineRepository commercialAreaStationLineRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final MeetingCoverStorage meetingCoverStorage;
    private final MeetingCoverProcessor meetingCoverProcessor;
    private final MeetingCoverCleanupProcessor meetingCoverCleanupProcessor;
    private final KakaoRouteProperties kakaoRouteProperties;
    private final KakaoRouteClient kakaoRouteClient;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            MeetingParticipantScheduleDateAvailabilityRepository meetingParticipantScheduleDateAvailabilityRepository,
            MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository,
            MeetingScheduleCandidateRepository meetingScheduleCandidateRepository,
            MeetingScheduleCandidateAvailabilityRepository meetingScheduleCandidateAvailabilityRepository,
            MeetingPlaceRecommendationSnapshotRepository meetingPlaceRecommendationSnapshotRepository,
            UserRepository userRepository,
            CommercialAreaCatalog commercialAreaCatalog,
            CommercialAreaStationLineRepository commercialAreaStationLineRepository,
            InviteCodeGenerator inviteCodeGenerator,
            PasswordEncoder passwordEncoder,
            MeetingCoverStorage meetingCoverStorage,
            MeetingCoverProcessor meetingCoverProcessor,
            MeetingCoverCleanupProcessor meetingCoverCleanupProcessor,
            KakaoRouteProperties kakaoRouteProperties,
            KakaoRouteClient kakaoRouteClient
    ) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingParticipantScheduleDateAvailabilityRepository = meetingParticipantScheduleDateAvailabilityRepository;
        this.meetingParticipantScheduleAvailabilityRepository = meetingParticipantScheduleAvailabilityRepository;
        this.meetingScheduleCandidateRepository = meetingScheduleCandidateRepository;
        this.meetingScheduleCandidateAvailabilityRepository = meetingScheduleCandidateAvailabilityRepository;
        this.meetingPlaceRecommendationSnapshotRepository = meetingPlaceRecommendationSnapshotRepository;
        this.userRepository = userRepository;
        this.commercialAreaCatalog = commercialAreaCatalog;
        this.commercialAreaStationLineRepository = commercialAreaStationLineRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.meetingCoverStorage = meetingCoverStorage;
        this.meetingCoverProcessor = meetingCoverProcessor;
        this.meetingCoverCleanupProcessor = meetingCoverCleanupProcessor;
        this.kakaoRouteProperties = kakaoRouteProperties;
        this.kakaoRouteClient = kakaoRouteClient;
    }

    @Transactional
    public MeetingCreateResult createMeeting(
            AuthenticatedMember hostMember,
            CreateMeetingCommand command,
            List<LocalDate> scheduleCandidateDates,
            SaveParticipationCommand participationCommand
    ) {
        return createMeeting(hostMember, command, scheduleCandidateDates, participationCommand, null);
    }

    @Transactional
    public MeetingCreateResult createMeeting(
            AuthenticatedMember hostMember,
            CreateMeetingCommand command,
            List<LocalDate> scheduleCandidateDates,
            SaveParticipationCommand participationCommand,
            MultipartFile coverImage
    ) {
        User hostUser = findActiveUserForUpdate(hostMember.userId());

        Meeting meeting = new Meeting(
                hostUser,
                normalizeRequired(command.name()),
                normalizeOptional(command.description()),
                command.maxParticipants(),
                command.planningType(),
                command.scheduleMode(),
                command.scheduleInputType(),
                resolveFixedScheduleAt(command),
                resolveAvailableStartTime(command),
                resolveAvailableEndTime(command),
                command.placeMode(),
                resolvePlaceRecommendationStrategy(command.placeMode()),
                resolveFixedPlaceName(command),
                resolveFixedPlaceAddress(command),
                resolveDeadlineAt(command),
                inviteCodeGenerator.generate()
        );
        Meeting savedMeeting = meetingRepository.saveAndFlush(meeting);
        MeetingParticipant hostParticipant = meetingParticipantRepository.saveAndFlush(
                MeetingParticipant.host(savedMeeting, hostUser)
        );
        saveHostScheduleCandidates(savedMeeting, scheduleCandidateDates);
        SaveParticipationCommand resolvedCommand = resolveHostCreationParticipationCommand(
                savedMeeting,
                scheduleCandidateDates,
                participationCommand
        );
        saveParticipation(savedMeeting, hostParticipant, resolvedCommand, false);
        saveScheduleCandidateAvailabilities(savedMeeting, resolvedCommand);

        if (coverImage != null && !coverImage.isEmpty()) {
            saveCoverImage(savedMeeting, coverImage);
        }
        return MeetingCreateResult.from(savedMeeting);
    }

    @Transactional
    public MeetingCoverResult replaceCoverImage(Long meetingId, AuthenticatedMember member, MultipartFile coverImage) {
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        validateCoverModificationAuthority(meeting, member);
        String previousKey = meeting.getCoverImageKey();
        Long cleanupTaskId = meetingCoverCleanupProcessor.createDeletionTask(previousKey);
        saveCoverImage(meeting, coverImage);
        processCleanupTaskAfterCommit(cleanupTaskId);
        return new MeetingCoverResult(MeetingCoverUrl.from(meeting));
    }

    @Transactional
    public void deleteCoverImage(Long meetingId, AuthenticatedMember member) {
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        validateCoverModificationAuthority(meeting, member);
        String previousKey = meeting.getCoverImageKey();
        if (previousKey == null) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_NOT_FOUND);
        }
        Long cleanupTaskId = meetingCoverCleanupProcessor.createDeletionTask(previousKey);
        meeting.removeCoverImage();
        processCleanupTaskAfterCommit(cleanupTaskId);
    }

    @Transactional
    public void deleteMeeting(Long meetingId, AuthenticatedMember member) {
        findActiveUserForUpdate(member.userId());
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_NOT_FOUND));
        if (!meeting.getHostUser().getId().equals(member.userId())) {
            throw new MoyeoException(MeetingErrorCode.MEETING_DELETION_FORBIDDEN);
        }

        Long cleanupTaskId = meetingCoverCleanupProcessor.createDeletionTask(meeting.getCoverImageKey());
        deletePlaceRecommendationSnapshots(meeting.getId());
        deleteMeetingParticipants(meeting.getId());
        meetingScheduleCandidateAvailabilityRepository.deleteAllByMeetingId(meeting.getId());
        meetingScheduleCandidateAvailabilityRepository.flush();
        meetingScheduleCandidateRepository.deleteAllByMeetingId(meeting.getId());
        meetingScheduleCandidateRepository.flush();
        meetingRepository.delete(meeting);
        meetingRepository.flush();
        processCleanupTaskAfterCommit(cleanupTaskId);
    }

    @Transactional
    public void leaveMeeting(Long meetingId, AuthenticatedMember member) {
        findActiveUserForUpdate(member.userId());
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_NOT_FOUND));
        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, member.userId())
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
        if (participant.getParticipantType() != ParticipantType.MEMBER) {
            throw new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_LEAVE_FORBIDDEN);
        }
        removeParticipantAndShrinkCapacity(meeting, participant);
    }

    @Transactional
    public void leaveGuest(String inviteCode, String nickname) {
        Meeting meeting = meetingRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
        MeetingParticipant participant = meetingParticipantRepository
                .findByMeetingIdAndNicknameAndParticipantType(
                        meeting.getId(),
                        normalizeRequired(nickname),
                        ParticipantType.GUEST
                )
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
        removeParticipantAndShrinkCapacity(meeting, participant);
    }

    @Transactional
    public MeetingParticipantNicknameResult updateMeetingParticipantNickname(
            Long meetingId,
            AuthenticatedMember member,
            String nickname
    ) {
        findActiveUserForUpdate(member.userId());
        meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_NOT_FOUND));
        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, member.userId())
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
        participant.changeNickname(normalizeRequired(nickname));
        return new MeetingParticipantNicknameResult(meetingId, participant.getId(), participant.getUser().getId(), participant.getNickname());
    }

    public MeetingCoverStorage.CoverObject getCoverImage(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        if (meeting.getCoverImageKey() == null) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_NOT_FOUND);
        }
        return meetingCoverStorage.get(meeting.getCoverImageKey());
    }

    private void saveCoverImage(Meeting meeting, MultipartFile coverImage) {
        byte[] resized = meetingCoverProcessor.resizeToJpeg(coverImage);
        String objectKey = "meeting-covers/" + UUID.randomUUID() + ".jpg";
        meetingCoverStorage.put(objectKey, resized);
        meeting.changeCoverImageKey(objectKey);
        deleteUploadedObjectOnRollback(objectKey);
    }

    private void validateCoverModificationAuthority(Meeting meeting, AuthenticatedMember member) {
        if (!meeting.getHostUser().getId().equals(member.userId())) {
            throw new MoyeoException(MeetingCoverErrorCode.MEETING_COVER_IMAGE_MODIFICATION_FORBIDDEN);
        }
    }

    private void deleteMeetingParticipants(Long meetingId) {
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId);
        for (MeetingParticipant participant : participants) {
            deleteParticipant(participant);
        }
    }

    private void removeParticipantAndShrinkCapacity(Meeting meeting, MeetingParticipant participant) {
        if (meetingParticipantRepository.countByMeetingId(meeting.getId()) == meeting.getMaxParticipants()) {
            deletePlaceRecommendationSnapshots(meeting.getId());
        }
        deleteParticipant(participant);
        meeting.decreaseMaxParticipants();
    }

    private void deletePlaceRecommendationSnapshots(Long meetingId) {
        meetingPlaceRecommendationSnapshotRepository.deleteAllByMeetingId(meetingId);
        meetingPlaceRecommendationSnapshotRepository.flush();
    }

    private void deleteParticipant(MeetingParticipant participant) {
        meetingParticipantScheduleDateAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleDateAvailabilityRepository.flush();
        meetingParticipantScheduleAvailabilityRepository.flush();
        meetingParticipantRepository.delete(participant);
        meetingParticipantRepository.flush();
    }

    private void deleteUploadedObjectOnRollback(String objectKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    meetingCoverCleanupProcessor.deleteOrEnqueue(objectKey);
                }
            }
        });
    }

    private void processCleanupTaskAfterCommit(Long taskId) {
        if (taskId == null) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                meetingCoverCleanupProcessor.process(List.of(taskId));
            }
        });
    }

    public MeetingInvitationResult getInvitation(String inviteCode, AuthenticatedMember member) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        long participantCount = meetingParticipantRepository.countByMeetingId(meeting.getId());
        List<MeetingScheduleCandidate> scheduleCandidates = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId());
        List<MeetingScheduleCandidateAvailability> scheduleCandidateAvailabilities =
                meetingScheduleCandidateAvailabilityRepository
                        .findAllByMeetingIdOrderByCandidateDateAndTimeAsc(meeting.getId());
        boolean alreadyJoined = member != null
                && meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), member.userId());
        return MeetingInvitationResult.from(
                meeting,
                findHostParticipantNickname(meeting),
                participantCount,
                scheduleCandidates,
                scheduleCandidateAvailabilities,
                alreadyJoined
        );
    }

    public Long validateInvitationExists(String inviteCode) {
        return findMeetingByInviteCode(inviteCode).getId();
    }

    public MeetingViewResult getMeetingView(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId());
        List<MeetingViewResult.Participant> participantResults = participants.stream()
                .map(participant -> new MeetingViewResult.Participant(
                        participant.getId(),
                        participant.getUser() == null ? null : participant.getUser().getId(),
                        participant.getNickname(),
                        participant.getParticipantType().name(),
                        participant.isWithdrawn()
                ))
                .toList();

        return new MeetingViewResult(
                meeting.getId(),
                meeting.getName(),
                meeting.getDescription(),
                MeetingCoverUrl.from(meeting),
                meeting.getPlanningType().name(),
                meeting.getStatus() == com.moyeo.domain.meeting.MeetingStatus.CONFIRMED,
                meeting.getScheduleMode().name(),
                meeting.getScheduleInputType().name(),
                meeting.getPlaceMode().name(),
                meeting.getPlaceRecommendationStrategy() != null ? meeting.getPlaceRecommendationStrategy().name() : null,
                meeting.getMaxParticipants(),
                participants.size(),
                meeting.getDeadlineAt(),
                remainingMinutes(meeting.getDeadlineAt()),
                meeting.getConfirmedScheduleDate(),
                meeting.getConfirmedStartTime(),
                meeting.getConfirmedEndTime(),
                meeting.getConfirmedPlaceName(),
                participantResults
        );
    }

    public MyMeetingListResult getMyMeetings(AuthenticatedMember member) {
        LocalDateTime now = LocalDateTime.now();
        List<MyMeetingListResult.Item> items = meetingParticipantRepository.findAllByUserIdWithMeeting(member.userId()).stream()
                .map(participant -> {
                    Meeting meeting = participant.getMeeting();
                    String deadlineStatus = meeting.getStatus() == com.moyeo.domain.meeting.MeetingStatus.CONFIRMED
                            ? "CLOSED"
                            : meeting.getDeadlineAt() == null ? "NO_DEADLINE"
                            : meeting.getDeadlineAt().isAfter(now) ? "OPEN" : "CLOSED";
                    LocalDateTime scheduledAt = meeting.getConfirmedScheduleDate() == null ? null
                            : LocalDateTime.of(meeting.getConfirmedScheduleDate(), meeting.getConfirmedStartTime() != null ? meeting.getConfirmedStartTime() : LocalTime.MIDNIGHT);
                    return new MyMeetingListResult.Item(meeting.getId(), meeting.getInviteCode(), meeting.getName(), MeetingCoverUrl.from(meeting),
                            findHostParticipantNickname(meeting), participant.getParticipantType().name(),
                            (int) meetingParticipantRepository.countByMeetingId(meeting.getId()),
                            meeting.getMaxParticipants(), deadlineStatus, meeting.getDeadlineAt(), meeting.getConfirmedAt(), scheduledAt,
                            meeting.getConfirmedScheduleDate(), meeting.getConfirmedStartTime(), meeting.getConfirmedEndTime(), meeting.getConfirmedPlaceName());
                }).toList();
        List<MyMeetingListResult.Item> planning = items.stream().filter(item -> item.confirmedAt() == null)
                .sorted(Comparator.comparing(MyMeetingListResult.Item::meetingId).reversed()).toList();
        List<MyMeetingListResult.Item> confirmedItems = items.stream().filter(item -> item.confirmedAt() != null).toList();
        List<MyMeetingListResult.Item> confirmed = new ArrayList<>();
        confirmed.addAll(confirmedItems.stream().filter(item -> item.scheduledAt() != null && !item.scheduledAt().isBefore(now))
                .sorted(Comparator.comparing(MyMeetingListResult.Item::scheduledAt)).toList());
        confirmed.addAll(confirmedItems.stream().filter(item -> item.scheduledAt() != null && item.scheduledAt().isBefore(now))
                .sorted(Comparator.comparing(MyMeetingListResult.Item::scheduledAt).reversed()).toList());
        confirmed.addAll(confirmedItems.stream().filter(item -> item.scheduledAt() == null)
                .sorted(Comparator.comparing(MyMeetingListResult.Item::confirmedAt).reversed()).toList());
        return new MyMeetingListResult(planning, confirmed);
    }

    public MyMeetingDetailResult getMyMeetingDetail(Long meetingId, AuthenticatedMember member) {
        MeetingParticipant currentParticipant = meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, member.userId())
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_NOT_FOUND));
        Meeting meeting = currentParticipant.getMeeting();
        List<MyMeetingDetailResult.Participant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId).stream()
                .map(participant -> new MyMeetingDetailResult.Participant(
                        participant.getId(),
                        participant.getUser() == null ? null : participant.getUser().getId(),
                        participant.getNickname(),
                        participant.getParticipantType().name(),
                        member.userId().equals(participant.getUser() == null ? null : participant.getUser().getId())
                ))
                .toList();
        return new MyMeetingDetailResult(
                meeting.getId(),
                meeting.getName(),
                meeting.getDescription(),
                MeetingCoverUrl.from(meeting),
                findHostParticipantNickname(meeting),
                meeting.getConfirmedScheduleDate(),
                meeting.getConfirmedStartTime(),
                meeting.getConfirmedEndTime(),
                meeting.getConfirmedPlaceName(),
                participants
        );
    }

    private String findHostParticipantNickname(Meeting meeting) {
        return meetingParticipantRepository.findByMeetingIdAndUserId(meeting.getId(), meeting.getHostUser().getId())
                .map(MeetingParticipant::getNickname)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
    }

    public MyParticipationResult getMyParticipation(String inviteCode, AuthenticatedMember member) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        MeetingParticipant participant = findMemberParticipant(meeting, member.userId());
        return toMyParticipationResult(meeting, participant);
    }

    public MyParticipationResult getGuestParticipation(String inviteCode, String nickname) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        MeetingParticipant participant = findGuestParticipant(meeting, nickname);
        return toMyParticipationResult(meeting, participant);
    }

    @Transactional
    public MyParticipationResult updateMyScheduleResponse(
            String inviteCode,
            AuthenticatedMember member,
            SaveParticipationCommand command
    ) {
        MeetingParticipant participant = findMemberParticipantForUpdate(inviteCode, member);
        Meeting meeting = participant.getMeeting();
        validateJoinOpen(meeting);
        validateScheduleResponseInput(meeting, command);
        saveScheduleResponse(meeting, participant, command, true);
        return toMyParticipationResult(meeting, participant);
    }

    @Transactional
    public MyParticipationResult updateMyDeparture(
            String inviteCode,
            AuthenticatedMember member,
            SaveParticipationCommand.Departure departure
    ) {
        MeetingParticipant participant = findMemberParticipantForUpdate(inviteCode, member);
        Meeting meeting = participant.getMeeting();
        validateJoinOpen(meeting);

        if (meeting.getPlaceMode() != PlaceMode.RECOMMEND || departure == null) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }

        String departureAddress = normalizeRequired(departure.address());
        validateSupportedDepartureRegion(departureAddress);
        participant.updateDeparture(
                normalizeOptional(departure.name()),
                departureAddress,
                departure.latitude(),
                departure.longitude(),
                departure.transportationMode()
        );
        return toMyParticipationResult(meeting, participant);
    }

    @Transactional
    public MyParticipationResult updateGuestScheduleResponse(
            String inviteCode,
            String nickname,
            SaveParticipationCommand command
    ) {
        MeetingParticipant participant = findGuestParticipantForUpdate(inviteCode, nickname);
        Meeting meeting = participant.getMeeting();
        validateJoinOpen(meeting);
        validateScheduleResponseInput(meeting, command);
        saveScheduleResponse(meeting, participant, command, true);
        return toMyParticipationResult(meeting, participant);
    }

    @Transactional
    public MyParticipationResult updateGuestDeparture(
            String inviteCode,
            String nickname,
            SaveParticipationCommand.Departure departure
    ) {
        MeetingParticipant participant = findGuestParticipantForUpdate(inviteCode, nickname);
        Meeting meeting = participant.getMeeting();
        validateJoinOpen(meeting);
        if (meeting.getPlaceMode() != PlaceMode.RECOMMEND || departure == null) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
        String departureAddress = normalizeRequired(departure.address());
        validateSupportedDepartureRegion(departureAddress);
        participant.updateDeparture(normalizeOptional(departure.name()), departureAddress, departure.latitude(),
                departure.longitude(), departure.transportationMode());
        return toMyParticipationResult(meeting, participant);
    }

    public ScheduleViewResult getScheduleView(String inviteCode, String sort) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        long participantCount = meetingParticipantRepository.countByMeetingId(meeting.getId());
        Map<Long, MeetingParticipant> participantsById = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId())
                .stream()
                .collect(Collectors.toMap(MeetingParticipant::getId, Function.identity()));
        String resolvedSort = resolveScheduleSort(sort);

        if (meeting.getScheduleInputType() == ScheduleInputType.DATE_ONLY) {
            return getDateOnlyScheduleView(meeting, participantCount, participantsById, resolvedSort);
        }

        List<MeetingParticipantScheduleAvailability> availabilities = meetingParticipantScheduleAvailabilityRepository
                .findAllByParticipantMeetingId(meeting.getId());
        Map<ScheduleSlot, Set<Long>> participantsBySlot = availabilities.stream()
                .flatMap(availability -> expandHourlyScheduleSlots(availability).stream())
                .collect(Collectors.groupingBy(
                        ParticipantScheduleSlot::slot,
                        Collectors.mapping(ParticipantScheduleSlot::participantId, Collectors.toSet())
                ));

        List<ScheduleViewResult.Candidate> availabilityBlocks = mergeConsecutiveScheduleCandidates(participantsBySlot, participantsById);

        return new ScheduleViewResult(
                meeting.getId(),
                meeting.getScheduleInputType().name(),
                meeting.getConfirmedScheduleDate() != null,
                resolvedSort,
                participantCount,
                selectRecommendedScheduleCandidates(availabilityBlocks, resolvedSort),
                availabilityBlocks.stream().map(this::toAvailabilityStatus).toList()
        );
    }

    private ScheduleViewResult getDateOnlyScheduleView(
            Meeting meeting,
            long participantCount,
            Map<Long, MeetingParticipant> participantsById,
            String resolvedSort
    ) {
        List<MeetingParticipantScheduleDateAvailability> availabilities =
                meetingParticipantScheduleDateAvailabilityRepository.findAllByParticipantMeetingId(meeting.getId());
        Map<LocalDate, Set<Long>> participantsByDate = availabilities.stream()
                .collect(Collectors.groupingBy(
                        availability -> availability.getScheduleCandidate().getCandidateDate(),
                        Collectors.mapping(
                                availability -> availability.getParticipant().getId(),
                                Collectors.toSet()
                        )
                ));

        List<ScheduleViewResult.Candidate> availabilityBlocks = participantsByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ScheduleViewResult.Candidate(
                        entry.getKey(),
                        null,
                        null,
                        entry.getValue().size(),
                        toAvailableParticipants(entry.getValue(), participantsById)
                ))
                .toList();

        return new ScheduleViewResult(
                meeting.getId(),
                meeting.getScheduleInputType().name(),
                meeting.getConfirmedScheduleDate() != null,
                resolvedSort,
                participantCount,
                selectRecommendedScheduleCandidates(availabilityBlocks, resolvedSort),
                availabilityBlocks.stream().map(this::toAvailabilityStatus).toList()
        );
    }

    @Transactional
    public PlaceViewResult getPlaceView(String inviteCode) {
        Meeting meeting = meetingRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId());
        List<MeetingParticipant> departureParticipants = participants.stream()
                .filter(this::hasDeparture)
                .toList();
        List<MeetingParticipant> coordinateParticipants = departureParticipants.stream()
                .filter(this::hasCoordinates)
                .toList();
        List<PlaceViewResult.ParticipantDeparture> participantResults = participants.stream()
                .map(participant -> new PlaceViewResult.ParticipantDeparture(
                        participant.getId(),
                        participant.getUser() == null ? null : participant.getUser().getId(),
                        participant.getNickname(),
                        participant.getParticipantType().name(),
                        participant.isWithdrawn(),
                        participant.getDepartureName() != null
                                ? participant.getDepartureName()
                                : participant.getDepartureAddress(),
                        participant.getDepartureAddress(),
                        participant.getTransportationMode() != null ? participant.getTransportationMode().name() : null
                ))
                .toList();

        String strategy = meeting.getPlaceRecommendationStrategy() != null
                ? meeting.getPlaceRecommendationStrategy().name()
                : null;
        if (meeting.getPlaceMode() != PlaceMode.RECOMMEND || meeting.getPlaceRecommendationStrategy() == null) {
            return emptyPlaceView(meeting, participants.size(), participantResults, strategy);
        }

        if (meeting.getPlaceRecommendationStrategy() == PlaceRecommendationStrategy.RANDOM) {
            List<CommercialArea> randomAreas = new ArrayList<>(commercialAreaCatalog.findAll());
            Collections.shuffle(randomAreas);
            List<PlaceViewResult.Recommendation> recommendations = randomAreas
                    .stream()
                    .limit(5)
                    .map(area -> recommendation(area, 0, null))
                    .toList();
            recommendations = rankRecommendations(recommendations);
            recommendations = attachStation(recommendations);
            return new PlaceViewResult(
                    meeting.getId(),
                    meeting.getConfirmedPlaceName() != null,
                    strategy,
                    "RANDOM_CATALOG_PREVIEW",
                    null,
                    participants.size(),
                    participantResults,
                    recommendations
            );
        }

        if (coordinateParticipants.isEmpty()) {
            return new PlaceViewResult(
                    meeting.getId(), meeting.getConfirmedPlaceName() != null, strategy, "COORDINATES_PENDING", null, participants.size(),
                    participantResults, List.of()
            );
        }

        PlaceViewResult.Coordinate center = averageCoordinate(coordinateParticipants);
        List<PlaceViewResult.Recommendation> recommendations = commercialAreaCatalog.findAll()
                .stream()
                .map(area -> scoreArea(area, coordinateParticipants))
                .sorted(Comparator.comparingLong(ScoredCommercialArea::score)
                        .thenComparing(scoredArea -> scoredArea.area().areaName()))
                .limit(kakaoRouteProperties.preliminaryCandidateCount())
                .map(scoredArea -> recommendation(
                        scoredArea.area(),
                        0,
                        scoredArea.averageStraightDistanceMeters()
                ))
                .toList();

        recommendations = rankRecommendations(recommendations);
        recommendations = attachStation(recommendations);
        boolean actualTravelTimeReady = participants.size() >= meeting.getMaxParticipants()
                && coordinateParticipants.size() == participants.size();
        if (actualTravelTimeReady) {
            recommendations = actualTimeRecommendations(meeting, participants, recommendations);
        }
        return new PlaceViewResult(
                meeting.getId(),
                meeting.getConfirmedPlaceName() != null,
                strategy,
                actualTravelTimeReady ? "ACTUAL_TRAVEL_TIME" : "STRAIGHT_LINE_PREVIEW",
                center,
                participants.size(),
                participantResults,
                recommendations
        );
    }

    private List<PlaceViewResult.Recommendation> actualTimeRecommendations(
            Meeting meeting,
            List<MeetingParticipant> participants,
            List<PlaceViewResult.Recommendation> preliminaryRecommendations
    ) {
        List<MeetingPlaceRecommendationSnapshot> snapshots = meetingPlaceRecommendationSnapshotRepository
                .findAllByMeetingIdOrderByRankAsc(meeting.getId());
        if (snapshots.isEmpty()) {
            List<MeetingPlaceRecommendationSnapshot> scored;
            try {
                scored = preliminaryRecommendations.stream()
                        .map(recommendation -> actualTimeSnapshot(meeting, participants, recommendation))
                        .sorted(Comparator.comparingLong(this::actualTimeScore)
                                .thenComparingLong(MeetingPlaceRecommendationSnapshot::getAverageTravelTimeSeconds)
                                .thenComparingLong(MeetingPlaceRecommendationSnapshot::getMaxTravelTimeSeconds))
                        .limit(kakaoRouteProperties.finalRecommendationCount())
                        .toList();
            } catch (KakaoRouteUnavailableException exception) {
                throw new MoyeoException(MeetingErrorCode.ACTUAL_ROUTE_RECOMMENDATION_UNAVAILABLE);
            }
            List<MeetingPlaceRecommendationSnapshot> ranked = new ArrayList<>();
            for (int index = 0; index < scored.size(); index++) {
                MeetingPlaceRecommendationSnapshot snapshot = scored.get(index);
                ranked.add(new MeetingPlaceRecommendationSnapshot(
                        meeting,
                        index + 1,
                        snapshot.getAreaCode(), snapshot.getAreaName(), snapshot.getCategoryName(),
                        snapshot.getLatitude(), snapshot.getLongitude(), snapshot.getGuName(), snapshot.getDongName(),
                        snapshot.getAverageStraightDistanceMeters(),
                        snapshot.getAverageTravelTimeSeconds(), snapshot.getMaxTravelTimeSeconds()
                ));
            }
            snapshots = meetingPlaceRecommendationSnapshotRepository.saveAll(ranked);
        }
        return attachStation(snapshots.stream()
                .map(snapshot -> new PlaceViewResult.Recommendation(
                        snapshot.getRank(), snapshot.getAreaCode(), snapshot.getAreaName(), snapshot.getCategoryName(),
                        snapshot.getLatitude(), snapshot.getLongitude(), snapshot.getGuName(), snapshot.getDongName(),
                        snapshot.getAverageStraightDistanceMeters(), snapshot.getAverageTravelTimeSeconds(),
                        snapshot.getMaxTravelTimeSeconds(), null))
                .toList());
    }

    private MeetingPlaceRecommendationSnapshot actualTimeSnapshot(Meeting meeting, List<MeetingParticipant> participants,
                                                                    PlaceViewResult.Recommendation recommendation) {
        java.util.LongSummaryStatistics statistics = participants.stream()
                .mapToLong(participant -> kakaoRouteClient.findShortestTravelTimeSeconds(
                        participant.getTransportationMode(), participant.getDepartureLatitude(), participant.getDepartureLongitude(),
                        recommendation.latitude(), recommendation.longitude()))
                .summaryStatistics();
        return new MeetingPlaceRecommendationSnapshot(meeting, 0, recommendation.areaCode(), recommendation.areaName(),
                recommendation.categoryName(), recommendation.latitude(), recommendation.longitude(), recommendation.guName(),
                recommendation.dongName(), recommendation.averageStraightDistanceMeters(),
                Math.round(statistics.getAverage()), statistics.getMax());
    }

    private long actualTimeScore(MeetingPlaceRecommendationSnapshot snapshot) {
        return snapshot.getAverageTravelTimeSeconds() + snapshot.getMaxTravelTimeSeconds();
    }

    @Transactional
    public MeetingConfirmationResult confirmSchedule(Long meetingId, AuthenticatedMember member, ConfirmScheduleCommand command) {
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_NOT_FOUND));
        validateConfirmationHostAndReadiness(meeting, member);
        if (meeting.getScheduleInputType() == ScheduleInputType.NONE || meeting.getConfirmedScheduleDate() != null) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_CONFIRMATION_INPUT);
        }
        if (command.scheduleDate() == null || !meetingScheduleCandidateRepository
                    .findAllByMeetingIdOrderByCandidateDateAsc(meetingId).stream()
                    .anyMatch(candidate -> candidate.getCandidateDate().equals(command.scheduleDate()))) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_CONFIRMATION_INPUT);
        }
        LocalTime startTime = null;
        LocalTime endTime = null;
        if (meeting.getScheduleInputType() == ScheduleInputType.DATE_AND_TIME) {
            if (command.startTime() == null || command.endTime() == null
                    || !command.startTime().isBefore(command.endTime())
                    || command.startTime().isBefore(meeting.getAvailableStartTime())
                    || command.endTime().isAfter(meeting.getAvailableEndTime())) {
                throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_CONFIRMATION_INPUT);
            }
            startTime = command.startTime();
            endTime = command.endTime();
        } else if (command.startTime() != null || command.endTime() != null) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_CONFIRMATION_INPUT);
        }
        meeting.confirmSchedule(command.scheduleDate(), startTime, endTime);
        completeConfirmationIfReady(meeting);
        return confirmationResult(meeting);
    }

    @Transactional
    public MeetingConfirmationResult confirmPlace(Long meetingId, AuthenticatedMember member, ConfirmPlaceCommand command) {
        Meeting meeting = meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_NOT_FOUND));
        validateConfirmationHostAndReadiness(meeting, member);
        if (meeting.getPlaceMode() != PlaceMode.RECOMMEND || meeting.getConfirmedPlaceName() != null
                || command.commercialAreaCode() == null || command.commercialAreaCode().isBlank()) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_CONFIRMATION_INPUT);
        }
        PlaceViewResult.Recommendation place = getPlaceView(meeting.getInviteCode()).recommendations().stream()
                .filter(recommendation -> recommendation.areaCode().equals(command.commercialAreaCode()))
                .findFirst().orElseThrow(() -> new MoyeoException(MeetingErrorCode.INVALID_MEETING_CONFIRMATION_INPUT));
        meeting.confirmPlace(place.areaName(), null, place.latitude(), place.longitude(), place.areaCode());
        completeConfirmationIfReady(meeting);
        return confirmationResult(meeting);
    }

    private void validateConfirmationHostAndReadiness(Meeting meeting, AuthenticatedMember member) {
        if (!meeting.getHostUser().getId().equals(member.userId())) {
            throw new MoyeoException(MeetingErrorCode.MEETING_CONFIRMATION_FORBIDDEN);
        }
        if (meeting.getStatus() == com.moyeo.domain.meeting.MeetingStatus.CONFIRMED) {
            throw new MoyeoException(MeetingErrorCode.MEETING_ALREADY_CONFIRMED);
        }
        if (meetingParticipantRepository.countByMeetingId(meeting.getId()) < 2) {
            throw new MoyeoException(MeetingErrorCode.MEETING_CONFIRMATION_NOT_READY);
        }
    }

    private void completeConfirmationIfReady(Meeting meeting) {
        boolean scheduleConfirmed = meeting.getScheduleInputType() == ScheduleInputType.NONE || meeting.getConfirmedScheduleDate() != null;
        boolean placeConfirmed = meeting.getPlaceMode() != PlaceMode.RECOMMEND || meeting.getConfirmedPlaceName() != null;
        if (scheduleConfirmed && placeConfirmed) {
            meeting.completeConfirmation();
        }
    }

    private MeetingConfirmationResult confirmationResult(Meeting meeting) {
        return new MeetingConfirmationResult(meeting.getId(), meeting.getStatus().name(), meeting.getConfirmedAt(),
                meeting.getConfirmedScheduleDate(), meeting.getConfirmedStartTime(), meeting.getConfirmedEndTime(),
                meeting.getConfirmedPlaceName());
    }

    @Transactional
    public ParticipantJoinResult joinGuest(
            String inviteCode,
            String nickname,
            String rawPassword,
            SaveParticipationCommand participationCommand
    ) {
        String normalizedNickname = normalizeRequired(nickname);
        String passwordHash = passwordEncoder.encode(rawPassword);

        Meeting meeting = prepareGuestJoinableMeeting(inviteCode, normalizedNickname);

        try {
            MeetingParticipant participant = meetingParticipantRepository.saveAndFlush(
                    MeetingParticipant.guest(meeting, normalizedNickname, passwordHash)
            );
            saveParticipation(meeting, participant, participationCommand, true);
            return ParticipantJoinResult.from(meeting, participant);
        } catch (DataIntegrityViolationException exception) {
            throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_NICKNAME);
        }
    }

    @Transactional(readOnly = true)
    public GuestEntryResult checkGuestEntry(String inviteCode, String nickname, String rawPassword) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        String normalizedNickname = normalizeRequired(nickname);

        return meetingParticipantRepository
                .findByMeetingIdAndNicknameAndParticipantType(meeting.getId(), normalizedNickname, ParticipantType.GUEST)
                .map(participant -> {
                    if (!passwordEncoder.matches(rawPassword, participant.getPasswordHash())) {
                        throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_NICKNAME);
                    }
                    return GuestEntryResult.existingGuest();
                })
                .orElseGet(GuestEntryResult::newGuest);
    }

    @Transactional
    public ParticipantJoinResult joinMember(
            String inviteCode,
            AuthenticatedMember member,
            String nickname,
            SaveParticipationCommand participationCommand
    ) {
        User user = findActiveUserForUpdate(member.userId());
        String normalizedNickname = normalizeRequired(nickname);

        Meeting meeting = prepareMemberJoinableMeeting(inviteCode);
        if (meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
            throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_MEMBER);
        }

        try {
            MeetingParticipant participant = meetingParticipantRepository.saveAndFlush(
                    MeetingParticipant.member(meeting, user, normalizedNickname)
            );
            saveParticipation(meeting, participant, participationCommand, true);
            return ParticipantJoinResult.from(meeting, participant);
        } catch (DataIntegrityViolationException exception) {
            if (meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), user.getId())) {
                throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_MEMBER);
            }
            throw exception;
        }
    }

    @Transactional
    public SaveParticipationResult saveParticipation(
            String inviteCode,
            Long participantId,
            SaveParticipationCommand command
    ) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
        MeetingParticipant participant = meetingParticipantRepository.findByIdAndMeetingId(participantId, meeting.getId())
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));

        validateJoinOpen(meeting);
        return saveParticipation(meeting, participant, command, true);
    }

    private SaveParticipationResult saveParticipation(
            Meeting meeting,
            MeetingParticipant participant,
            SaveParticipationCommand command,
            boolean restrictScheduleToSnapshot
    ) {
        boolean requiresPlace = meeting.getPlaceMode() == PlaceMode.RECOMMEND;
        validateParticipationInput(meeting, command, requiresPlace);

        int scheduleAvailabilityCount = saveScheduleResponse(meeting, participant, command, restrictScheduleToSnapshot);
        boolean hasDeparture = false;

        if (requiresPlace) {
            SaveParticipationCommand.Departure departure = command.departure();
            String departureAddress = normalizeRequired(departure.address());
            validateSupportedDepartureRegion(departureAddress);
            participant.updateDeparture(
                    normalizeOptional(departure.name()),
                    departureAddress,
                    departure.latitude(),
                    departure.longitude(),
                    departure.transportationMode()
            );
            hasDeparture = true;
        }

        return new SaveParticipationResult(
                meeting.getId(),
                participant.getId(),
                participant.getUser() == null ? null : participant.getUser().getId(),
                scheduleAvailabilityCount,
                hasDeparture
        );
    }

    private Meeting findMeetingByInviteCode(String inviteCode) {
        return meetingRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
    }

    private MeetingParticipant findMemberParticipant(Meeting meeting, Long userId) {
        return meetingParticipantRepository.findByMeetingIdAndUserId(meeting.getId(), userId)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
    }

    private MeetingParticipant findGuestParticipant(Meeting meeting, String nickname) {
        return meetingParticipantRepository
                .findByMeetingIdAndNicknameAndParticipantType(meeting.getId(), normalizeRequired(nickname), ParticipantType.GUEST)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
    }

    private MeetingParticipant findMemberParticipantForUpdate(String inviteCode, AuthenticatedMember member) {
        User user = findActiveUserForUpdate(member.userId());
        Meeting meeting = findMeetingByInviteCodeForUpdate(inviteCode);
        return findMemberParticipant(meeting, user.getId());
    }

    private MeetingParticipant findGuestParticipantForUpdate(String inviteCode, String nickname) {
        Meeting meeting = findMeetingByInviteCodeForUpdate(inviteCode);
        return meetingParticipantRepository
                .findByMeetingIdAndNicknameAndParticipantType(meeting.getId(), normalizeRequired(nickname), ParticipantType.GUEST)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
    }

    private MyParticipationResult toMyParticipationResult(Meeting meeting, MeetingParticipant participant) {
        MyParticipationResult.ScheduleResponse scheduleResponse = switch (meeting.getScheduleInputType()) {
            case DATE_ONLY -> new MyParticipationResult.ScheduleResponse(
                    meetingParticipantScheduleDateAvailabilityRepository
                            .findAllByParticipantIdOrderByCandidateDateAsc(participant.getId())
                            .stream()
                            .map(availability -> availability.getScheduleCandidate().getCandidateDate())
                            .toList(),
                    List.of()
            );
            case DATE_AND_TIME -> new MyParticipationResult.ScheduleResponse(
                    List.of(),
                    meetingParticipantScheduleAvailabilityRepository
                            .findAllByParticipantIdOrderByCandidateDateAndTimeAsc(participant.getId())
                            .stream()
                            .map(availability -> new MyParticipationResult.ScheduleAvailability(
                                    availability.getScheduleCandidate().getCandidateDate(),
                                    availability.getStartTime(),
                                    availability.getEndTime()
                            ))
                            .toList()
            );
            case NONE -> null;
        };
        MyParticipationResult.Departure departure = meeting.getPlaceMode() == PlaceMode.RECOMMEND
                ? new MyParticipationResult.Departure(
                        participant.getDepartureName(),
                        participant.getDepartureAddress(),
                        participant.getDepartureLatitude(),
                        participant.getDepartureLongitude(),
                        participant.getTransportationMode().name()
                )
                : null;
        return new MyParticipationResult(
                meeting.getId(),
                participant.getParticipantType().name(),
                meeting.getScheduleInputType().name(),
                scheduleResponse,
                departure
        );
    }

    private User findActiveUserForUpdate(Long userId) {
        return userRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));
    }

    private Meeting findMeetingByInviteCodeForUpdate(String inviteCode) {
        return meetingRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
    }

    private Meeting prepareGuestJoinableMeeting(String inviteCode, String normalizedNickname) {
        Meeting meeting = findMeetingByInviteCodeForUpdate(inviteCode);
        validateJoinOpen(meeting);
        validateParticipantLimit(meeting);
        validateGuestNicknameAvailable(meeting, normalizedNickname);
        return meeting;
    }

    private Meeting prepareMemberJoinableMeeting(String inviteCode) {
        Meeting meeting = findMeetingByInviteCodeForUpdate(inviteCode);
        validateJoinOpen(meeting);
        validateParticipantLimit(meeting);
        return meeting;
    }

    private void validateJoinOpen(Meeting meeting) {
        if (meeting.getStatus() == com.moyeo.domain.meeting.MeetingStatus.CONFIRMED) {
            throw new MoyeoException(MeetingErrorCode.MEETING_PARTICIPATION_CLOSED);
        }
        if (meeting.getDeadlineAt() != null && !meeting.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new MoyeoException(MeetingErrorCode.MEETING_PARTICIPATION_CLOSED);
        }
    }

    private void validateParticipantLimit(Meeting meeting) {
        if (meetingParticipantRepository.countByMeetingId(meeting.getId()) >= meeting.getMaxParticipants()) {
            throw new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_LIMIT_EXCEEDED);
        }
    }

    private void validateGuestNicknameAvailable(Meeting meeting, String normalizedNickname) {
        if (meetingParticipantRepository.existsByMeetingAndNicknameAndParticipantType(
                meeting,
                normalizedNickname,
                ParticipantType.GUEST
        )) {
            throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_NICKNAME);
        }
    }

    private boolean hasDeparture(MeetingParticipant participant) {
        return participant.getDepartureAddress() != null
                && participant.getTransportationMode() != null;
    }

    private boolean hasCoordinates(MeetingParticipant participant) {
        return participant.getDepartureLatitude() != null && participant.getDepartureLongitude() != null;
    }

    private Long remainingMinutes(LocalDateTime deadlineAt) {
        if (deadlineAt == null) {
            return null;
        }
        return Math.max(0, ChronoUnit.MINUTES.between(LocalDateTime.now(), deadlineAt));
    }

    private LocalDateTime resolveDeadlineAt(CreateMeetingCommand command) {
        return command.noDeadline() ? null : LocalDateTime.now().plusMinutes(command.deadlineMinutes());
    }

    private String resolveScheduleSort(String sort) {
        if ("LONGEST_MEETING".equals(sort) || "EARLIEST_DATE".equals(sort)) {
            return sort;
        }
        throw new MoyeoException(CommonErrorCode.INVALID_REQUEST);
    }

    private List<ScheduleViewResult.Candidate> mergeConsecutiveScheduleCandidates(
            Map<ScheduleSlot, Set<Long>> participantsBySlot,
            Map<Long, MeetingParticipant> participantsById
    ) {
        List<ScheduleSlotAvailability> slots = participantsBySlot.entrySet().stream()
                .map(entry -> new ScheduleSlotAvailability(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing((ScheduleSlotAvailability availability) -> availability.slot().candidateDate())
                        .thenComparing(availability -> availability.slot().startTime())
                        .thenComparing(availability -> availability.slot().endTime()))
                .toList();
        if (slots.isEmpty()) {
            return List.of();
        }

        List<ScheduleViewResult.Candidate> candidates = new ArrayList<>();
        ScheduleSlot currentSlot = slots.getFirst().slot();
        Set<Long> currentParticipantIds = slots.getFirst().participantIds();

        for (int index = 1; index < slots.size(); index++) {
            ScheduleSlotAvailability next = slots.get(index);
            if (canMergeScheduleSlots(currentSlot, currentParticipantIds, next)) {
                currentSlot = new ScheduleSlot(
                        currentSlot.candidateDate(),
                        currentSlot.startTime(),
                        next.slot().endTime()
                );
                continue;
            }
            candidates.add(toScheduleCandidate(currentSlot, currentParticipantIds, participantsById));
            currentSlot = next.slot();
            currentParticipantIds = next.participantIds();
        }
        candidates.add(toScheduleCandidate(currentSlot, currentParticipantIds, participantsById));
        return candidates;
    }

    private boolean canMergeScheduleSlots(
            ScheduleSlot currentSlot,
            Set<Long> currentParticipantIds,
            ScheduleSlotAvailability next
    ) {
        return currentSlot.candidateDate().equals(next.slot().candidateDate())
                && currentSlot.endTime().equals(next.slot().startTime())
                && currentParticipantIds.equals(next.participantIds());
    }

    private List<ParticipantScheduleSlot> expandHourlyScheduleSlots(
            MeetingParticipantScheduleAvailability availability
    ) {
        List<ParticipantScheduleSlot> slots = new ArrayList<>();
        for (LocalTime startTime = availability.getStartTime(); startTime.isBefore(availability.getEndTime()); startTime = startTime.plusHours(1)) {
            slots.add(new ParticipantScheduleSlot(
                    new ScheduleSlot(
                            availability.getScheduleCandidate().getCandidateDate(),
                            startTime,
                            startTime.plusHours(1)
                    ),
                    availability.getParticipant().getId()
            ));
        }
        return slots;
    }

    private ScheduleViewResult.Candidate toScheduleCandidate(
            ScheduleSlot slot,
            Set<Long> participantIds,
            Map<Long, MeetingParticipant> participantsById
    ) {
        return new ScheduleViewResult.Candidate(
                slot.candidateDate(),
                slot.startTime(),
                slot.endTime(),
                participantIds.size(),
                toAvailableParticipants(participantIds, participantsById)
        );
    }

    private List<ScheduleViewResult.Candidate> selectRecommendedScheduleCandidates(
            List<ScheduleViewResult.Candidate> availabilityBlocks,
            String sort
    ) {
        return availabilityBlocks.stream()
                .filter(candidate -> candidate.availableParticipantCount() >= 1)
                .sorted(Comparator.comparing(
                        ScheduleViewResult.Candidate::availableParticipantCount,
                        Comparator.reverseOrder()
                ).thenComparing(scheduleCandidateComparator(sort)))
                .limit(5)
                .toList();
    }

    private List<ScheduleViewResult.AvailableParticipant> toAvailableParticipants(
            Set<Long> participantIds,
            Map<Long, MeetingParticipant> participantsById
    ) {
        return participantIds.stream()
                .sorted()
                .map(participantsById::get)
                .filter(java.util.Objects::nonNull)
                .map(participant -> new ScheduleViewResult.AvailableParticipant(
                        participant.getId(),
                        participant.getUser() == null ? null : participant.getUser().getId(),
                        participant.getNickname()
                ))
                .toList();
    }

    private ScheduleViewResult.AvailabilityStatus toAvailabilityStatus(ScheduleViewResult.Candidate candidate) {
        return new ScheduleViewResult.AvailabilityStatus(
                candidate.candidateDate(),
                candidate.startTime(),
                candidate.endTime(),
                candidate.availableParticipantCount()
        );
    }

    private Comparator<ScheduleViewResult.Candidate> scheduleCandidateComparator(String sort) {
        if ("EARLIEST_DATE".equals(sort)) {
            return Comparator.comparing(ScheduleViewResult.Candidate::candidateDate)
                    .thenComparing(ScheduleViewResult.Candidate::startTime)
                    .thenComparing(ScheduleViewResult.Candidate::endTime)
                    .thenComparing(ScheduleViewResult.Candidate::availableParticipantCount, Comparator.reverseOrder());
        }
        return Comparator.comparing(
                        (ScheduleViewResult.Candidate candidate) -> candidate.startTime() == null
                                ? 0L
                                : ChronoUnit.MINUTES.between(candidate.startTime(), candidate.endTime()),
                        Comparator.reverseOrder()
                )
                .thenComparing(ScheduleViewResult.Candidate::candidateDate)
                .thenComparing(ScheduleViewResult.Candidate::startTime);
    }

    private PlaceViewResult emptyPlaceView(
            Meeting meeting,
            long participantCount,
            List<PlaceViewResult.ParticipantDeparture> participantResults,
            String strategy
    ) {
        return new PlaceViewResult(
                meeting.getId(),
                meeting.getConfirmedPlaceName() != null,
                strategy,
                strategy != null ? "STRAIGHT_LINE_PREVIEW" : null,
                null,
                participantCount,
                participantResults,
                List.of()
        );
    }

    private PlaceViewResult.Coordinate averageCoordinate(List<MeetingParticipant> participants) {
        BigDecimal latitude = participants.stream()
                .map(MeetingParticipant::getDepartureLatitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(participants.size()), 7, RoundingMode.HALF_UP);
        BigDecimal longitude = participants.stream()
                .map(MeetingParticipant::getDepartureLongitude)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(participants.size()), 7, RoundingMode.HALF_UP);
        return new PlaceViewResult.Coordinate(latitude, longitude);
    }

    private ScoredCommercialArea scoreArea(CommercialArea area, List<MeetingParticipant> participants) {
        CommercialAreaPreliminaryScoreCalculator.Score score = CommercialAreaPreliminaryScoreCalculator.calculate(
                area,
                participants.stream()
                        .map(participant -> new CommercialAreaPreliminaryScoreCalculator.ParticipantDeparture(
                                participant.getDepartureLatitude(),
                                participant.getDepartureLongitude(),
                                participant.getTransportationMode()
                        ))
                        .toList()
        );
        return new ScoredCommercialArea(
                area,
                score.averageStraightDistanceMeters(),
                score.score()
        );
    }

    private List<PlaceViewResult.Recommendation> rankRecommendations(List<PlaceViewResult.Recommendation> recommendations) {
        List<PlaceViewResult.Recommendation> ranked = new ArrayList<>();
        for (int index = 0; index < recommendations.size(); index++) {
            PlaceViewResult.Recommendation recommendation = recommendations.get(index);
            ranked.add(recommendation(
                    new CommercialArea(
                            recommendation.areaCode(),
                            recommendation.areaName(),
                            recommendation.categoryName(),
                            recommendation.latitude(),
                            recommendation.longitude(),
                            recommendation.guName(),
                            recommendation.dongName()
                    ),
                    index + 1,
                    recommendation.averageStraightDistanceMeters(),
                    recommendation.averageTravelTimeSeconds(),
                    recommendation.maxTravelTimeSeconds()
            ));
        }
        return ranked;
    }

    private PlaceViewResult.Recommendation recommendation(
            CommercialArea area,
            int rank,
            Long averageStraightDistanceMeters
    ) {
        return recommendation(area, rank, averageStraightDistanceMeters, null, null);
    }

    private PlaceViewResult.Recommendation recommendation(
            CommercialArea area,
            int rank,
            Long averageStraightDistanceMeters,
            Long averageTravelTimeSeconds,
            Long maxTravelTimeSeconds
    ) {
        return new PlaceViewResult.Recommendation(
                rank,
                area.areaCode(),
                area.areaName(),
                area.categoryName(),
                area.latitude(),
                area.longitude(),
                area.guName(),
                area.dongName(),
                averageStraightDistanceMeters,
                averageTravelTimeSeconds,
                maxTravelTimeSeconds,
                null
        );
    }

    private List<PlaceViewResult.Recommendation> attachStation(
            List<PlaceViewResult.Recommendation> recommendations
    ) {
        if (recommendations.isEmpty()) {
            return recommendations;
        }
        Map<String, PlaceViewResult.Station> stationsByAreaCode = commercialAreaStationLineRepository
                .findAllForCommercialAreaCodes(
                        CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS,
                        recommendations.stream().map(PlaceViewResult.Recommendation::areaCode).toList()
                )
                .stream()
                .collect(Collectors.groupingBy(
                        stationLine -> stationLine.getCommercialArea().getExternalCode(),
                        Collectors.collectingAndThen(Collectors.toList(), this::toStation)
                ));
        return recommendations.stream()
                .map(recommendation -> new PlaceViewResult.Recommendation(
                        recommendation.rank(),
                        recommendation.areaCode(),
                        recommendation.areaName(),
                        recommendation.categoryName(),
                        recommendation.latitude(),
                        recommendation.longitude(),
                        recommendation.guName(),
                        recommendation.dongName(),
                        recommendation.averageStraightDistanceMeters(),
                        recommendation.averageTravelTimeSeconds(),
                        recommendation.maxTravelTimeSeconds(),
                        stationsByAreaCode.get(recommendation.areaCode())
                ))
                .toList();
    }

    private PlaceViewResult.Station toStation(List<CommercialAreaStationLineEntity> stationLines) {
        CommercialAreaStationLineEntity stationLine = stationLines.getFirst();
        return new PlaceViewResult.Station(
                stationLine.getStationName(),
                stationLines.stream().map(CommercialAreaStationLineEntity::getLineName).toList()
        );
    }

    private void saveHostScheduleCandidates(Meeting meeting, List<LocalDate> scheduleCandidateDates) {
        boolean requiresSchedule = meeting.getScheduleMode() == ScheduleMode.VOTE;
        boolean hasScheduleCandidateDates = scheduleCandidateDates != null && !scheduleCandidateDates.isEmpty();
        if (requiresSchedule != hasScheduleCandidateDates) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
        if (!requiresSchedule) {
            return;
        }

        List<MeetingScheduleCandidate> candidates = scheduleCandidateDates.stream()
                .distinct()
                .sorted()
                .map(candidateDate -> new MeetingScheduleCandidate(meeting, candidateDate))
                .toList();
        meetingScheduleCandidateRepository.saveAllAndFlush(candidates);
    }

    private SaveParticipationCommand resolveHostCreationParticipationCommand(
            Meeting meeting,
            List<LocalDate> scheduleCandidateDates,
            SaveParticipationCommand command
    ) {
        if (meeting.getScheduleInputType() != ScheduleInputType.DATE_ONLY) {
            return command;
        }
        if (!command.scheduleAvailableDates().isEmpty() || !command.scheduleAvailabilities().isEmpty()) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
        return new SaveParticipationCommand(
                scheduleCandidateDates != null ? scheduleCandidateDates : List.of(),
                List.of(),
                command.departure()
        );
    }

    private void saveScheduleCandidateAvailabilities(Meeting meeting, SaveParticipationCommand command) {
        if (meeting.getScheduleInputType() != ScheduleInputType.DATE_AND_TIME) {
            return;
        }

        Map<LocalDate, MeetingScheduleCandidate> candidatesByDate = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId())
                .stream()
                .collect(Collectors.toMap(MeetingScheduleCandidate::getCandidateDate, Function.identity()));
        LinkedHashSet<ScheduleSlot> slots = command.scheduleAvailabilities().stream()
                .map(availability -> new ScheduleSlot(
                        availability.candidateDate(),
                        availability.startTime(),
                        availability.endTime()
                ))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<MeetingScheduleCandidateAvailability> snapshots = slots.stream()
                .map(slot -> new MeetingScheduleCandidateAvailability(
                        candidatesByDate.get(slot.candidateDate()),
                        slot.startTime(),
                        slot.endTime()
                ))
                .toList();
        meetingScheduleCandidateAvailabilityRepository.saveAll(snapshots);
    }

    private void validateParticipationInput(
            Meeting meeting,
            SaveParticipationCommand command,
            boolean requiresPlace
    ) {
        boolean hasAvailableDates = command.scheduleAvailableDates() != null
                && !command.scheduleAvailableDates().isEmpty();
        boolean hasScheduleAvailabilities = command.scheduleAvailabilities() != null
                && !command.scheduleAvailabilities().isEmpty();
        boolean hasDeparture = command.departure() != null;

        boolean validScheduleInput = switch (meeting.getScheduleInputType()) {
            case DATE_ONLY -> hasAvailableDates && !hasScheduleAvailabilities;
            case DATE_AND_TIME -> !hasAvailableDates && hasScheduleAvailabilities;
            case NONE -> !hasAvailableDates && !hasScheduleAvailabilities;
        };
        if (!validScheduleInput || requiresPlace != hasDeparture) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
    }

    private void validateScheduleResponseInput(Meeting meeting, SaveParticipationCommand command) {
        boolean hasAvailableDates = command.scheduleAvailableDates() != null
                && !command.scheduleAvailableDates().isEmpty();
        boolean hasScheduleAvailabilities = command.scheduleAvailabilities() != null
                && !command.scheduleAvailabilities().isEmpty();
        boolean validScheduleInput = switch (meeting.getScheduleInputType()) {
            case DATE_ONLY -> hasAvailableDates && !hasScheduleAvailabilities;
            case DATE_AND_TIME -> !hasAvailableDates && hasScheduleAvailabilities;
            case NONE -> false;
        };
        if (!validScheduleInput) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
    }

    private int saveScheduleResponse(
            Meeting meeting,
            MeetingParticipant participant,
            SaveParticipationCommand command,
            boolean restrictScheduleToSnapshot
    ) {
        meetingParticipantScheduleDateAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleDateAvailabilityRepository.flush();
        meetingParticipantScheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleAvailabilityRepository.flush();

        Map<LocalDate, MeetingScheduleCandidate> candidatesByDate = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId())
                .stream()
                .collect(Collectors.toMap(MeetingScheduleCandidate::getCandidateDate, Function.identity()));
        Map<LocalDate, List<MeetingScheduleCandidateAvailability>> snapshotsByDate = restrictScheduleToSnapshot
                ? meetingScheduleCandidateAvailabilityRepository
                        .findAllByMeetingIdOrderByCandidateDateAndTimeAsc(meeting.getId())
                        .stream()
                        .collect(Collectors.groupingBy(
                                availability -> availability.getScheduleCandidate().getCandidateDate(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ))
                : null;

        if (meeting.getScheduleInputType() == ScheduleInputType.DATE_ONLY) {
            return saveScheduleDateAvailabilities(participant, command, candidatesByDate);
        }
        if (meeting.getScheduleInputType() == ScheduleInputType.NONE) {
            return 0;
        }

        LinkedHashSet<ScheduleSlot> slots = new LinkedHashSet<>();
        for (SaveParticipationCommand.ScheduleAvailability availability : command.scheduleAvailabilities()) {
            validateScheduleAvailability(meeting, candidatesByDate, snapshotsByDate, availability);
            slots.add(new ScheduleSlot(
                    availability.candidateDate(),
                    availability.startTime(),
                    availability.endTime()
            ));
        }

        List<MeetingParticipantScheduleAvailability> entities = slots.stream()
                .map(slot -> new MeetingParticipantScheduleAvailability(
                        participant,
                        candidatesByDate.get(slot.candidateDate()),
                        slot.startTime(),
                        slot.endTime()
                ))
                .toList();
        meetingParticipantScheduleAvailabilityRepository.saveAll(entities);
        return entities.size();
    }

    private int saveScheduleDateAvailabilities(
            MeetingParticipant participant,
            SaveParticipationCommand command,
            Map<LocalDate, MeetingScheduleCandidate> candidatesByDate
    ) {
        LinkedHashSet<LocalDate> availableDates = new LinkedHashSet<>(command.scheduleAvailableDates());
        if (!candidatesByDate.keySet().containsAll(availableDates)) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
        List<MeetingParticipantScheduleDateAvailability> entities = availableDates.stream()
                .map(candidateDate -> new MeetingParticipantScheduleDateAvailability(
                        participant,
                        candidatesByDate.get(candidateDate)
                ))
                .toList();
        meetingParticipantScheduleDateAvailabilityRepository.saveAll(entities);
        return entities.size();
    }

    private void validateScheduleAvailability(
            Meeting meeting,
            Map<LocalDate, MeetingScheduleCandidate> candidatesByDate,
            Map<LocalDate, List<MeetingScheduleCandidateAvailability>> snapshotsByDate,
            SaveParticipationCommand.ScheduleAvailability availability
    ) {
        if (!candidatesByDate.containsKey(availability.candidateDate())
                || availability.startTime() == null
                || availability.endTime() == null
                || !availability.startTime().isBefore(availability.endTime())
                || !isHourUnit(availability.startTime())
                || !isHourUnit(availability.endTime())
                || availability.startTime().isBefore(meeting.getAvailableStartTime())
                || availability.endTime().isAfter(meeting.getAvailableEndTime())
                || !isWithinScheduleSnapshot(snapshotsByDate, availability)) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
    }

    private boolean isWithinScheduleSnapshot(
            Map<LocalDate, List<MeetingScheduleCandidateAvailability>> snapshotsByDate,
            SaveParticipationCommand.ScheduleAvailability availability
    ) {
        if (snapshotsByDate == null) {
            return true;
        }

        List<MeetingScheduleCandidateAvailability> snapshots = snapshotsByDate.get(availability.candidateDate());
        if (snapshots == null) {
            return false;
        }
        for (LocalTime time = availability.startTime(); time.isBefore(availability.endTime()); time = time.plusHours(1)) {
            LocalTime slotStartTime = time;
            LocalTime slotEndTime = time.plusHours(1);
            boolean allowed = snapshots.stream().anyMatch(snapshot -> !snapshot.getStartTime().isAfter(slotStartTime)
                    && !snapshot.getEndTime().isBefore(slotEndTime));
            if (!allowed) {
                return false;
            }
        }
        return true;
    }

    private LocalDateTime resolveFixedScheduleAt(CreateMeetingCommand command) {
        return command.scheduleMode() == ScheduleMode.FIXED ? command.fixedScheduleAt() : null;
    }

    private LocalTime resolveAvailableStartTime(CreateMeetingCommand command) {
        return command.scheduleInputType() == ScheduleInputType.DATE_AND_TIME ? command.availableStartTime() : null;
    }

    private LocalTime resolveAvailableEndTime(CreateMeetingCommand command) {
        return command.scheduleInputType() == ScheduleInputType.DATE_AND_TIME ? command.availableEndTime() : null;
    }

    private PlaceRecommendationStrategy resolvePlaceRecommendationStrategy(PlaceMode placeMode) {
        return placeMode == PlaceMode.RECOMMEND ? PlaceRecommendationStrategy.MIDDLE_POINT : null;
    }

    private String resolveFixedPlaceName(CreateMeetingCommand command) {
        return command.placeMode() == PlaceMode.FIXED ? normalizeOptional(command.fixedPlaceName()) : null;
    }

    private String resolveFixedPlaceAddress(CreateMeetingCommand command) {
        return command.placeMode() == PlaceMode.FIXED ? normalizeOptional(command.fixedPlaceAddress()) : null;
    }

    private String normalizeRequired(String value) {
        return value.strip();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private void validateSupportedDepartureRegion(String address) {
        if (DepartureRegionPolicy.isSupportedAddress(address)) {
            return;
        }
        throw new MoyeoException(MeetingErrorCode.UNSUPPORTED_DEPARTURE_REGION);
    }

    private boolean isHourUnit(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    private record ScheduleSlot(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }

    private record ScheduleSlotAvailability(
            ScheduleSlot slot,
            Set<Long> participantIds
    ) {
    }

    private record ParticipantScheduleSlot(
            ScheduleSlot slot,
            Long participantId
    ) {
    }

    private record ScoredCommercialArea(
            CommercialArea area,
            long averageStraightDistanceMeters,
            long score
    ) {
    }
}
