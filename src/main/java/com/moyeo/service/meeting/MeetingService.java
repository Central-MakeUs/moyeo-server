package com.moyeo.service.meeting;

import com.moyeo.domain.member.User;
import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaStationLineEntity;
import com.moyeo.domain.departure.DeparturePlaceSearch;
import com.moyeo.domain.meeting.ParticipantType;
import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlaceRecommendationStrategy;
import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.MeetingParticipant;
import com.moyeo.domain.meeting.MeetingParticipantScheduleDateAvailability;
import com.moyeo.domain.meeting.MeetingParticipantScheduleAvailability;
import com.moyeo.domain.meeting.MeetingScheduleCandidate;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
import com.moyeo.route.KakaoRouteProperties;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.commercial.CommercialAreaStationLineRepository;
import com.moyeo.repository.departure.DeparturePlaceSearchRepository;
import com.moyeo.repository.meeting.MeetingParticipantRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleDateAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.meeting.MeetingScheduleCandidateRepository;
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
    private final DeparturePlaceSearchRepository departurePlaceSearchRepository;
    private final UserRepository userRepository;
    private final CommercialAreaCatalog commercialAreaCatalog;
    private final CommercialAreaStationLineRepository commercialAreaStationLineRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final MeetingCoverStorage meetingCoverStorage;
    private final MeetingCoverProcessor meetingCoverProcessor;
    private final MeetingCoverCleanupProcessor meetingCoverCleanupProcessor;
    private final KakaoRouteProperties kakaoRouteProperties;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            MeetingParticipantScheduleDateAvailabilityRepository meetingParticipantScheduleDateAvailabilityRepository,
            MeetingParticipantScheduleAvailabilityRepository meetingParticipantScheduleAvailabilityRepository,
            MeetingScheduleCandidateRepository meetingScheduleCandidateRepository,
            DeparturePlaceSearchRepository departurePlaceSearchRepository,
            UserRepository userRepository,
            CommercialAreaCatalog commercialAreaCatalog,
            CommercialAreaStationLineRepository commercialAreaStationLineRepository,
            InviteCodeGenerator inviteCodeGenerator,
            PasswordEncoder passwordEncoder,
            MeetingCoverStorage meetingCoverStorage,
            MeetingCoverProcessor meetingCoverProcessor,
            MeetingCoverCleanupProcessor meetingCoverCleanupProcessor,
            KakaoRouteProperties kakaoRouteProperties
    ) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.meetingParticipantScheduleDateAvailabilityRepository = meetingParticipantScheduleDateAvailabilityRepository;
        this.meetingParticipantScheduleAvailabilityRepository = meetingParticipantScheduleAvailabilityRepository;
        this.meetingScheduleCandidateRepository = meetingScheduleCandidateRepository;
        this.departurePlaceSearchRepository = departurePlaceSearchRepository;
        this.userRepository = userRepository;
        this.commercialAreaCatalog = commercialAreaCatalog;
        this.commercialAreaStationLineRepository = commercialAreaStationLineRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.meetingCoverStorage = meetingCoverStorage;
        this.meetingCoverProcessor = meetingCoverProcessor;
        this.meetingCoverCleanupProcessor = meetingCoverCleanupProcessor;
        this.kakaoRouteProperties = kakaoRouteProperties;
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
        saveParticipation(savedMeeting, hostParticipant, resolvedCommand);

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
        deleteMeetingSearchHistory(meeting.getId());
        deleteMeetingParticipants(meeting.getId());
        meetingScheduleCandidateRepository.deleteAllByMeetingId(meeting.getId());
        meetingScheduleCandidateRepository.flush();
        meetingRepository.delete(meeting);
        meetingRepository.flush();
        processCleanupTaskAfterCommit(cleanupTaskId);
    }

    @Transactional
    public void leaveMeeting(Long meetingId, AuthenticatedMember member) {
        findActiveUserForUpdate(member.userId());
        meetingRepository.findByIdForUpdate(meetingId)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_NOT_FOUND));
        MeetingParticipant participant = meetingParticipantRepository.findByMeetingIdAndUserId(meetingId, member.userId())
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_NOT_FOUND));
        if (participant.getParticipantType() != ParticipantType.MEMBER) {
            throw new MoyeoException(MeetingErrorCode.MEETING_PARTICIPANT_LEAVE_FORBIDDEN);
        }
        deleteParticipant(participant);
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
        return new MeetingParticipantNicknameResult(meetingId, participant.getId(), participant.getNickname());
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

    private void deleteMeetingSearchHistory(Long meetingId) {
        List<DeparturePlaceSearch> searches = departurePlaceSearchRepository.findAllByMeetingIdIn(List.of(meetingId));
        if (!searches.isEmpty()) {
            departurePlaceSearchRepository.deleteAll(searches);
            departurePlaceSearchRepository.flush();
        }
    }

    private void deleteMeetingParticipants(Long meetingId) {
        List<MeetingParticipant> participants = meetingParticipantRepository.findAllByMeetingIdOrderByIdAsc(meetingId);
        for (MeetingParticipant participant : participants) {
            deleteParticipant(participant);
        }
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
        boolean alreadyJoined = member != null
                && meetingParticipantRepository.existsByMeetingIdAndUserId(meeting.getId(), member.userId());
        return MeetingInvitationResult.from(meeting, participantCount, scheduleCandidates, alreadyJoined);
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
                meeting.getScheduleMode().name(),
                meeting.getScheduleInputType().name(),
                meeting.getPlaceMode().name(),
                meeting.getPlaceRecommendationStrategy() != null ? meeting.getPlaceRecommendationStrategy().name() : null,
                meeting.getMaxParticipants(),
                participants.size(),
                meeting.getDeadlineAt(),
                remainingMinutes(meeting.getDeadlineAt()),
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
                            meeting.getHostUser().getNickname(), participant.getParticipantType().name(),
                            (int) meetingParticipantRepository.countByMeetingId(meeting.getId()),
                            meeting.getMaxParticipants(), deadlineStatus, meeting.getDeadlineAt(), meeting.getConfirmedAt(), scheduledAt,
                            meeting.getConfirmedScheduleDate(), meeting.getConfirmedStartTime(), meeting.getConfirmedEndTime(), meeting.getConfirmedPlaceName());
                }).toList();
        List<MyMeetingListResult.Item> planning = items.stream().filter(item -> item.confirmedAt() == null)
                .sorted(Comparator.comparing((MyMeetingListResult.Item item) -> !"CLOSED".equals(item.deadlineStatus()))
                        .thenComparing(item -> item.deadlineAt() == null ? LocalDateTime.MAX : item.deadlineAt())).toList();
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
                meeting.getHostUser().getNickname(),
                meeting.getConfirmedScheduleDate(),
                meeting.getConfirmedStartTime(),
                meeting.getConfirmedEndTime(),
                meeting.getConfirmedPlaceName(),
                participants
        );
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
                resolvedSort,
                participantCount,
                selectBestScheduleCandidates(availabilityBlocks, resolvedSort),
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
                resolvedSort,
                participantCount,
                selectBestScheduleCandidates(availabilityBlocks, resolvedSort),
                availabilityBlocks.stream().map(this::toAvailabilityStatus).toList()
        );
    }

    public PlaceViewResult getPlaceView(String inviteCode) {
        Meeting meeting = findMeetingByInviteCode(inviteCode);
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
                    meeting.getId(), strategy, "COORDINATES_PENDING", null, participants.size(),
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
        return new PlaceViewResult(
                meeting.getId(),
                strategy,
                "STRAIGHT_LINE_PREVIEW",
                center,
                participants.size(),
                participantResults,
                recommendations
        );
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
            saveParticipation(meeting, participant, participationCommand);
            return ParticipantJoinResult.from(meeting, participant);
        } catch (DataIntegrityViolationException exception) {
            throw new MoyeoException(MeetingErrorCode.DUPLICATE_MEETING_PARTICIPANT_NICKNAME);
        }
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
            saveParticipation(meeting, participant, participationCommand);
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
        return saveParticipation(meeting, participant, command);
    }

    private SaveParticipationResult saveParticipation(
            Meeting meeting,
            MeetingParticipant participant,
            SaveParticipationCommand command
    ) {
        boolean requiresPlace = meeting.getPlaceMode() == PlaceMode.RECOMMEND;
        validateParticipationInput(meeting, command, requiresPlace);

        int scheduleAvailabilityCount = saveScheduleResponse(meeting, participant, command);
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

        return new SaveParticipationResult(meeting.getId(), participant.getId(), scheduleAvailabilityCount, hasDeparture);
    }

    private Meeting findMeetingByInviteCode(String inviteCode) {
        return meetingRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new MoyeoException(MeetingErrorCode.MEETING_INVITATION_NOT_FOUND));
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

    private List<ScheduleViewResult.Candidate> selectBestScheduleCandidates(
            List<ScheduleViewResult.Candidate> availabilityBlocks,
            String sort
    ) {
        long maxAvailableParticipantCount = availabilityBlocks.stream()
                .mapToLong(ScheduleViewResult.Candidate::availableParticipantCount)
                .max()
                .orElse(0L);
        if (maxAvailableParticipantCount < 2) {
            return List.of();
        }

        return availabilityBlocks.stream()
                .filter(candidate -> candidate.availableParticipantCount() == maxAvailableParticipantCount)
                .sorted(scheduleCandidateComparator(sort))
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
                    recommendation.averageStraightDistanceMeters()
            ));
        }
        return ranked;
    }

    private PlaceViewResult.Recommendation recommendation(
            CommercialArea area,
            int rank,
            Long averageStraightDistanceMeters
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

    private int saveScheduleResponse(
            Meeting meeting,
            MeetingParticipant participant,
            SaveParticipationCommand command
    ) {
        meetingParticipantScheduleDateAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleDateAvailabilityRepository.flush();
        meetingParticipantScheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());
        meetingParticipantScheduleAvailabilityRepository.flush();

        Map<LocalDate, MeetingScheduleCandidate> candidatesByDate = meetingScheduleCandidateRepository
                .findAllByMeetingIdOrderByCandidateDateAsc(meeting.getId())
                .stream()
                .collect(Collectors.toMap(MeetingScheduleCandidate::getCandidateDate, Function.identity()));

        if (meeting.getScheduleInputType() == ScheduleInputType.DATE_ONLY) {
            return saveScheduleDateAvailabilities(participant, command, candidatesByDate);
        }
        if (meeting.getScheduleInputType() == ScheduleInputType.NONE) {
            return 0;
        }

        LinkedHashSet<ScheduleSlot> slots = new LinkedHashSet<>();
        for (SaveParticipationCommand.ScheduleAvailability availability : command.scheduleAvailabilities()) {
            validateScheduleAvailability(meeting, candidatesByDate, availability);
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
            SaveParticipationCommand.ScheduleAvailability availability
    ) {
        if (!candidatesByDate.containsKey(availability.candidateDate())
                || availability.startTime() == null
                || availability.endTime() == null
                || !availability.startTime().isBefore(availability.endTime())
                || !isHourUnit(availability.startTime())
                || !isHourUnit(availability.endTime())
                || availability.startTime().isBefore(meeting.getAvailableStartTime())
                || availability.endTime().isAfter(meeting.getAvailableEndTime())) {
            throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
        }
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
        if (address.equals("서울")
                || address.startsWith("서울 ")
                || address.equals("서울특별시")
                || address.startsWith("서울특별시 ")
                || address.equals("경기")
                || address.startsWith("경기 ")
                || address.equals("경기도")
                || address.startsWith("경기도 ")) {
            return;
        }
        throw new MoyeoException(MeetingErrorCode.INVALID_MEETING_PARTICIPATION_INPUT);
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
