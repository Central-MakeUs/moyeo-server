package com.moyeo.service.meeting;

import com.moyeo.service.member.AuthenticatedMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(propagation = Propagation.NEVER)
public class MeetingCoverApplicationService {
    private static final Logger log = LoggerFactory.getLogger(MeetingCoverApplicationService.class);
    private final MeetingService meetingService;
    private final MeetingCoverProcessor coverProcessor;
    private final MeetingCoverStorage storage;
    private final MeetingCoverCleanupProcessor cleanupProcessor;

    public MeetingCoverApplicationService(MeetingService meetingService, MeetingCoverProcessor coverProcessor,
            MeetingCoverStorage storage, MeetingCoverCleanupProcessor cleanupProcessor) {
        this.meetingService = meetingService;
        this.coverProcessor = coverProcessor;
        this.storage = storage;
        this.cleanupProcessor = cleanupProcessor;
    }

    public MeetingCreateResult createMeeting(AuthenticatedMember member, CreateMeetingCommand command,
            List<LocalDate> scheduleCandidateDates, SaveParticipationCommand participation, MultipartFile coverImage) {
        MeetingCreateResult created = meetingService.createMeeting(member, command, scheduleCandidateDates, participation);
        if (coverImage == null || coverImage.isEmpty()) return created;
        MeetingCoverUpload upload = null;
        try {
            upload = upload(coverImage);
            if (!meetingService.attachInitialCover(created.meetingId(), member, upload)) {
                cleanupProcessor.deleteOrEnqueue(upload.objectKey());
            }
        } catch (RuntimeException exception) {
            compensateConfirmedRollback(upload);
            log.warn("Meeting created without optional cover. meetingId={} errorType={}",
                    created.meetingId(), exception.getClass().getSimpleName());
        }
        return created;
    }

    public MeetingCoverStorage.CoverObject getCoverImage(String inviteCode) {
        String objectKey = meetingService.findCoverImageKey(inviteCode);
        return storage.get(objectKey);
    }

    public MeetingCoverResult replaceCoverImage(Long meetingId, AuthenticatedMember member, MultipartFile coverImage) {
        meetingService.validateCoverReplacement(meetingId, member);
        MeetingCoverUpload upload = upload(coverImage);
        MeetingService.CoverReplacement replacement;
        try {
            replacement = meetingService.replaceCoverImage(meetingId, member, upload);
        } catch (RuntimeException exception) {
            compensateConfirmedRollback(upload);
            throw exception;
        }
        processCleanup(replacement.cleanupTaskId());
        return replacement.result();
    }

    public void deleteCoverImage(Long meetingId, AuthenticatedMember member) {
        processCleanup(meetingService.deleteCoverImage(meetingId, member));
    }

    public void deleteMeeting(Long meetingId, AuthenticatedMember member) {
        processCleanup(meetingService.deleteMeeting(meetingId, member));
    }

    private MeetingCoverUpload upload(MultipartFile coverImage) {
        byte[] resized = coverProcessor.resizeToJpeg(coverImage);
        String objectKey = "meeting-covers/" + UUID.randomUUID() + ".jpg";
        try {
            storage.put(objectKey, resized);
        } catch (RuntimeException exception) {
            // No DB reference was written. A timed-out PUT can still have created the object.
            cleanupProcessor.enqueueUnreferencedUpload(objectKey, exception);
            throw exception;
        }
        return new MeetingCoverUpload(objectKey);
    }

    private void compensateConfirmedRollback(MeetingCoverUpload upload) {
        if (upload != null && upload.wasRolledBack()) {
            cleanupProcessor.deleteOrEnqueue(upload.objectKey());
        }
        // Unknown completion is recovered only by the reference-checked orphan scan.
    }

    private void processCleanup(Long taskId) {
        if (taskId != null) cleanupProcessor.process(List.of(taskId));
    }
}
