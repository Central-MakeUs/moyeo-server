package com.moyeo.service.meeting;

import com.moyeo.service.member.AuthenticatedMember;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingCoverApplicationServiceTest {
    @Mock MeetingService meetings;
    @Mock MeetingCoverProcessor images;
    @Mock MeetingCoverStorage storage;
    @Mock MeetingCoverCleanupProcessor cleanup;
    @Mock AuthenticatedMember member;
    MeetingCoverApplicationService application;
    final MockMultipartFile file = new MockMultipartFile("coverImage", "test.jpg", "image/jpeg", new byte[]{1});

    @BeforeEach
    void setUp() {
        application = new MeetingCoverApplicationService(meetings, images, storage, cleanup);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void replacementCompensatesOnlyAConfirmedRollback(int completionStatus) {
        when(images.resizeToJpeg(file)).thenReturn(new byte[]{1});
        when(meetings.replaceCoverImage(eq(1L), eq(member), any(MeetingCoverUpload.class)))
                .thenAnswer(invocation -> {
                    MeetingCoverUpload upload = invocation.getArgument(2);
                    TransactionSynchronizationManager.initSynchronization();
                    try {
                        upload.trackTransactionCompletion();
                        TransactionSynchronizationManager.getSynchronizations()
                                .forEach(synchronization -> synchronization.afterCompletion(completionStatus));
                    } finally {
                        TransactionSynchronizationManager.clearSynchronization();
                    }
                    throw new IllegalStateException("transaction completion failed");
                });
        assertThatThrownBy(() -> application.replaceCoverImage(1L, member, file))
                .isInstanceOf(IllegalStateException.class);
        var key = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(storage).put(key.capture(), any(byte[].class));
        if (completionStatus == 1) verify(cleanup).deleteOrEnqueue(key.getValue());
        else verify(cleanup, never()).deleteOrEnqueue(anyString());
    }

    @Test
    void replacementRejectsUnauthorizedRequestBeforeUploading() {
        doThrow(new IllegalStateException("forbidden")).when(meetings).validateCoverReplacement(1L, member);
        assertThatThrownBy(() -> application.replaceCoverImage(1L, member, file)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(images, storage, cleanup);
    }

    @Test
    void readUsesTheKeyFromTheCompletedDatabaseLookup() {
        when(meetings.findCoverImageKey("invite")).thenReturn("meeting-covers/a.jpg");
        var cover = new MeetingCoverStorage.CoverObject(new byte[]{1}, "image/jpeg");
        when(storage.get("meeting-covers/a.jpg")).thenReturn(cover);
        assertThat(application.getCoverImage("invite")).isSameAs(cover);
        var order = inOrder(meetings, storage);
        order.verify(meetings).findCoverImageKey("invite");
        order.verify(storage).get("meeting-covers/a.jpg");
    }

    @Test
    void deleteAttemptsCleanupOnlyAfterTheLocalChangeCompletes() {
        when(meetings.deleteCoverImage(1L, member)).thenReturn(7L);
        application.deleteCoverImage(1L, member);
        var order = inOrder(meetings, cleanup);
        order.verify(meetings).deleteCoverImage(1L, member);
        order.verify(cleanup).process(List.of(7L));
    }
}
