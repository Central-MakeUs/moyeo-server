package com.moyeo.service.room;

import com.moyeo.domain.member.User;
import com.moyeo.domain.room.ParticipantType;
import com.moyeo.domain.room.PlaceMode;
import com.moyeo.domain.room.PlaceRecommendationStrategy;
import com.moyeo.domain.room.Room;
import com.moyeo.domain.room.RoomParticipant;
import com.moyeo.domain.room.RoomParticipantScheduleAvailability;
import com.moyeo.domain.room.RoomScheduleCandidate;
import com.moyeo.domain.room.ScheduleMode;
import com.moyeo.global.error.CommonErrorCode;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.room.RoomParticipantRepository;
import com.moyeo.repository.room.RoomParticipantScheduleAvailabilityRepository;
import com.moyeo.repository.room.RoomRepository;
import com.moyeo.repository.room.RoomScheduleCandidateRepository;
import com.moyeo.service.member.AuthenticatedMember;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomParticipantScheduleAvailabilityRepository roomParticipantScheduleAvailabilityRepository;
    private final RoomScheduleCandidateRepository roomScheduleCandidateRepository;
    private final UserRepository userRepository;
    private final InviteCodeGenerator inviteCodeGenerator;
    private final PasswordEncoder passwordEncoder;

    public RoomService(
            RoomRepository roomRepository,
            RoomParticipantRepository roomParticipantRepository,
            RoomParticipantScheduleAvailabilityRepository roomParticipantScheduleAvailabilityRepository,
            RoomScheduleCandidateRepository roomScheduleCandidateRepository,
            UserRepository userRepository,
            InviteCodeGenerator inviteCodeGenerator,
            PasswordEncoder passwordEncoder
    ) {
        this.roomRepository = roomRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.roomParticipantScheduleAvailabilityRepository = roomParticipantScheduleAvailabilityRepository;
        this.roomScheduleCandidateRepository = roomScheduleCandidateRepository;
        this.userRepository = userRepository;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RoomCreateResult createRoom(
            AuthenticatedMember hostMember,
            CreateRoomCommand command
    ) {
        User hostUser = userRepository.findById(hostMember.userId())
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));

        Room room = new Room(
                hostUser,
                normalizeRequired(command.name()),
                normalizeOptional(command.description()),
                command.maxParticipants(),
                command.planningType(),
                command.scheduleMode(),
                resolveFixedScheduleAt(command),
                resolveAvailableStartTime(command),
                resolveAvailableEndTime(command),
                command.placeMode(),
                resolvePlaceRecommendationStrategy(command),
                resolveFixedPlaceName(command),
                resolveFixedPlaceAddress(command),
                LocalDateTime.now().plusMinutes(command.deadlineMinutes()),
                inviteCodeGenerator.generate()
        );
        Room savedRoom = roomRepository.saveAndFlush(room);
        saveScheduleCandidates(savedRoom, command);

        RoomParticipant hostParticipant = roomParticipantRepository.saveAndFlush(
                RoomParticipant.host(
                        savedRoom,
                        hostUser,
                        normalizeOptional(command.hostDepartureName()),
                        normalizeOptional(command.hostDepartureAddress()),
                        command.hostDepartureLatitude(),
                        command.hostDepartureLongitude(),
                        command.hostTransportationMode()
                )
        );

        List<RoomScheduleCandidate> scheduleCandidates = roomScheduleCandidateRepository
                .findAllByRoomIdOrderByCandidateDateAsc(savedRoom.getId());
        return RoomCreateResult.from(savedRoom, hostParticipant, scheduleCandidates);
    }

    public RoomInvitationResult getInvitation(String inviteCode) {
        Room room = findRoomByInviteCode(inviteCode);
        long participantCount = roomParticipantRepository.countByRoomId(room.getId());
        List<RoomScheduleCandidate> scheduleCandidates = roomScheduleCandidateRepository
                .findAllByRoomIdOrderByCandidateDateAsc(room.getId());
        return RoomInvitationResult.from(room, participantCount, scheduleCandidates);
    }

    @Transactional
    public ParticipantJoinResult joinGuest(String inviteCode, String nickname, String rawPassword) {
        String normalizedNickname = normalizeRequired(nickname);
        String passwordHash = passwordEncoder.encode(rawPassword);

        Room room = prepareGuestJoinableRoom(inviteCode, normalizedNickname);

        try {
            RoomParticipant participant = roomParticipantRepository.saveAndFlush(
                    RoomParticipant.guest(room, normalizedNickname, passwordHash)
            );
            return ParticipantJoinResult.from(room, participant);
        } catch (DataIntegrityViolationException exception) {
            throw new MoyeoException(RoomErrorCode.DUPLICATE_ROOM_PARTICIPANT_NICKNAME);
        }
    }

    @Transactional
    public ParticipantJoinResult joinMember(
            String inviteCode,
            AuthenticatedMember member,
            String nickname,
            String rawPassword
    ) {
        User user = userRepository.findById(member.userId())
                .orElseThrow(() -> new MoyeoException(CommonErrorCode.INVALID_REQUEST));
        String normalizedNickname = normalizeRequired(nickname);
        String passwordHash = passwordEncoder.encode(rawPassword);

        Room room = prepareMemberJoinableRoom(inviteCode);
        if (roomParticipantRepository.existsByRoomIdAndUserId(room.getId(), user.getId())) {
            throw new MoyeoException(RoomErrorCode.DUPLICATE_ROOM_PARTICIPANT_MEMBER);
        }

        try {
            RoomParticipant participant = roomParticipantRepository.saveAndFlush(
                    RoomParticipant.member(room, user, normalizedNickname, passwordHash)
            );
            return ParticipantJoinResult.from(room, participant);
        } catch (DataIntegrityViolationException exception) {
            if (roomParticipantRepository.existsByRoomIdAndUserId(room.getId(), user.getId())) {
                throw new MoyeoException(RoomErrorCode.DUPLICATE_ROOM_PARTICIPANT_MEMBER);
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
        Room room = findRoomByInviteCode(inviteCode);
        RoomParticipant participant = roomParticipantRepository.findByIdAndRoomId(participantId, room.getId())
                .orElseThrow(() -> new MoyeoException(RoomErrorCode.ROOM_PARTICIPANT_NOT_FOUND));

        if (!room.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new MoyeoException(RoomErrorCode.ROOM_PARTICIPATION_CLOSED);
        }

        boolean requiresSchedule = room.getScheduleMode() == ScheduleMode.VOTE;
        boolean requiresPlace = room.getPlaceMode() == PlaceMode.RECOMMEND;
        validateParticipationInput(command, requiresSchedule, requiresPlace);

        int scheduleAvailabilityCount = saveScheduleAvailabilities(room, participant, command);
        boolean hasDeparture = false;

        if (requiresPlace) {
            SaveParticipationCommand.Departure departure = command.departure();
            participant.updateDeparture(
                    normalizeRequired(departure.name()),
                    normalizeRequired(departure.address()),
                    departure.latitude(),
                    departure.longitude(),
                    departure.transportationMode()
            );
            hasDeparture = true;
        }

        return new SaveParticipationResult(room.getId(), participant.getId(), scheduleAvailabilityCount, hasDeparture);
    }

    private Room findRoomByInviteCode(String inviteCode) {
        return roomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new MoyeoException(RoomErrorCode.ROOM_INVITATION_NOT_FOUND));
    }

    private Room findRoomByInviteCodeForUpdate(String inviteCode) {
        return roomRepository.findByInviteCodeForUpdate(inviteCode)
                .orElseThrow(() -> new MoyeoException(RoomErrorCode.ROOM_INVITATION_NOT_FOUND));
    }

    private Room prepareGuestJoinableRoom(String inviteCode, String normalizedNickname) {
        Room room = findRoomByInviteCodeForUpdate(inviteCode);
        validateJoinOpen(room);
        validateParticipantLimit(room);
        validateGuestNicknameAvailable(room, normalizedNickname);
        return room;
    }

    private Room prepareMemberJoinableRoom(String inviteCode) {
        Room room = findRoomByInviteCodeForUpdate(inviteCode);
        validateJoinOpen(room);
        validateParticipantLimit(room);
        return room;
    }

    private void validateJoinOpen(Room room) {
        if (!room.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new MoyeoException(RoomErrorCode.ROOM_PARTICIPATION_CLOSED);
        }
    }

    private void validateParticipantLimit(Room room) {
        if (roomParticipantRepository.countByRoomId(room.getId()) >= room.getMaxParticipants()) {
            throw new MoyeoException(RoomErrorCode.ROOM_PARTICIPANT_LIMIT_EXCEEDED);
        }
    }

    private void validateGuestNicknameAvailable(Room room, String normalizedNickname) {
        if (roomParticipantRepository.existsByRoomAndNicknameAndParticipantType(
                room,
                normalizedNickname,
                ParticipantType.GUEST
        )) {
            throw new MoyeoException(RoomErrorCode.DUPLICATE_ROOM_PARTICIPANT_NICKNAME);
        }
    }

    private void saveScheduleCandidates(Room room, CreateRoomCommand command) {
        if (command.scheduleMode() == ScheduleMode.VOTE) {
            List<RoomScheduleCandidate> candidates = command.scheduleCandidateDates().stream()
                    .distinct()
                    .sorted()
                    .map(candidateDate -> new RoomScheduleCandidate(room, candidateDate))
                    .toList();
            roomScheduleCandidateRepository.saveAll(candidates);
        }
    }

    private void validateParticipationInput(
            SaveParticipationCommand command,
            boolean requiresSchedule,
            boolean requiresPlace
    ) {
        boolean hasScheduleAvailabilities = command.scheduleAvailabilities() != null
                && !command.scheduleAvailabilities().isEmpty();
        boolean hasDeparture = command.departure() != null;

        if (requiresSchedule != hasScheduleAvailabilities || requiresPlace != hasDeparture) {
            throw new MoyeoException(RoomErrorCode.INVALID_ROOM_PARTICIPATION_INPUT);
        }
    }

    private int saveScheduleAvailabilities(
            Room room,
            RoomParticipant participant,
            SaveParticipationCommand command
    ) {
        roomParticipantScheduleAvailabilityRepository.deleteAllByParticipantId(participant.getId());

        if (room.getScheduleMode() != ScheduleMode.VOTE) {
            return 0;
        }

        Map<LocalDate, RoomScheduleCandidate> candidatesByDate = roomScheduleCandidateRepository
                .findAllByRoomIdOrderByCandidateDateAsc(room.getId())
                .stream()
                .collect(Collectors.toMap(RoomScheduleCandidate::getCandidateDate, Function.identity()));

        LinkedHashSet<ScheduleSlot> slots = new LinkedHashSet<>();
        for (SaveParticipationCommand.ScheduleAvailability availability : command.scheduleAvailabilities()) {
            validateScheduleAvailability(room, candidatesByDate, availability);
            slots.add(new ScheduleSlot(
                    availability.candidateDate(),
                    availability.startTime(),
                    availability.endTime()
            ));
        }

        List<RoomParticipantScheduleAvailability> entities = slots.stream()
                .map(slot -> new RoomParticipantScheduleAvailability(
                        participant,
                        candidatesByDate.get(slot.candidateDate()),
                        slot.startTime(),
                        slot.endTime()
                ))
                .toList();
        roomParticipantScheduleAvailabilityRepository.saveAll(entities);
        return entities.size();
    }

    private void validateScheduleAvailability(
            Room room,
            Map<LocalDate, RoomScheduleCandidate> candidatesByDate,
            SaveParticipationCommand.ScheduleAvailability availability
    ) {
        if (!candidatesByDate.containsKey(availability.candidateDate())
                || availability.startTime() == null
                || availability.endTime() == null
                || !availability.startTime().isBefore(availability.endTime())
                || !isHourUnit(availability.startTime())
                || !isHourUnit(availability.endTime())
                || availability.startTime().isBefore(room.getAvailableStartTime())
                || availability.endTime().isAfter(room.getAvailableEndTime())) {
            throw new MoyeoException(RoomErrorCode.INVALID_ROOM_PARTICIPATION_INPUT);
        }
    }

    private LocalDateTime resolveFixedScheduleAt(CreateRoomCommand command) {
        return command.scheduleMode() == ScheduleMode.FIXED ? command.fixedScheduleAt() : null;
    }

    private LocalTime resolveAvailableStartTime(CreateRoomCommand command) {
        return command.scheduleMode() == ScheduleMode.VOTE ? command.availableStartTime() : null;
    }

    private LocalTime resolveAvailableEndTime(CreateRoomCommand command) {
        return command.scheduleMode() == ScheduleMode.VOTE ? command.availableEndTime() : null;
    }

    private PlaceRecommendationStrategy resolvePlaceRecommendationStrategy(CreateRoomCommand command) {
        return command.placeMode() == PlaceMode.RECOMMEND ? command.placeRecommendationStrategy() : null;
    }

    private String resolveFixedPlaceName(CreateRoomCommand command) {
        return command.placeMode() == PlaceMode.FIXED ? normalizeOptional(command.fixedPlaceName()) : null;
    }

    private String resolveFixedPlaceAddress(CreateRoomCommand command) {
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

    private boolean isHourUnit(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    private record ScheduleSlot(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
