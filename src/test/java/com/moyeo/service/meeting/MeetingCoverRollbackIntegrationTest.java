package com.moyeo.service.meeting;

import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.meeting.PlaceMode;
import com.moyeo.domain.meeting.PlanningType;
import com.moyeo.domain.meeting.ScheduleInputType;
import com.moyeo.domain.meeting.ScheduleMode;
import com.moyeo.domain.member.User;
import com.moyeo.repository.meeting.MeetingCoverCleanupTaskRepository;
import com.moyeo.repository.meeting.MeetingRepository;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.service.member.AuthenticatedMember;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("local")
class MeetingCoverRollbackIntegrationTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingCoverCleanupTaskRepository cleanupTaskRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private MeetingCoverStorage meetingCoverStorage;

    @Test
    void uploadedReplacementIsDeletedWhenTheDatabaseTransactionRollsBack() throws Exception {
        User host = userRepository.saveAndFlush(new User("rollback-host"));
        Meeting meeting = meetingRepository.saveAndFlush(new Meeting(
                host,
                "rollback-test",
                null,
                4,
                PlanningType.SCHEDULE_ONLY,
                ScheduleMode.NONE,
                ScheduleInputType.NONE,
                null,
                null,
                null,
                PlaceMode.NONE,
                null,
                null,
                null,
                LocalDateTime.now().plusDays(1),
                "RB" + Long.toString(System.nanoTime(), 36)
        ));
        meeting.changeCoverImageKey("meeting-covers/original.jpg");
        meetingRepository.saveAndFlush(meeting);
        MockMultipartFile replacementImage = pngFile();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            meetingService.replaceCoverImage(
                    meeting.getId(),
                    AuthenticatedMember.from(host, true),
                    replacementImage
            );
            status.setRollbackOnly();
        });

        ArgumentCaptor<String> uploadedKey = ArgumentCaptor.forClass(String.class);
        verify(meetingCoverStorage).put(uploadedKey.capture(), any(byte[].class));
        verify(meetingCoverStorage).delete(uploadedKey.getValue());
        assertThat(cleanupTaskRepository.count()).isZero();

        entityManager.clear();
        assertThat(meetingRepository.findById(meeting.getId()))
                .get()
                .extracting(Meeting::getCoverImageKey)
                .isEqualTo("meeting-covers/original.jpg");
    }

    private MockMultipartFile pngFile() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("coverImage", "cover.png", "image/png", output.toByteArray());
    }
}
