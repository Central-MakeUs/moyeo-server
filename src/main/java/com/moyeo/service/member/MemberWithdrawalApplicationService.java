package com.moyeo.service.member;

import org.springframework.stereotype.Service;
import com.moyeo.service.meeting.MeetingCoverCleanupProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.NEVER)
public class MemberWithdrawalApplicationService {
    private final MemberWithdrawalService withdrawalService;
    private final KakaoUnlinkProcessor kakaoUnlinkProcessor;
    private final AppleRevokeProcessor appleRevokeProcessor;
    private final MeetingCoverCleanupProcessor coverCleanupProcessor;

    public MemberWithdrawalApplicationService(
            MemberWithdrawalService withdrawalService,
            KakaoUnlinkProcessor kakaoUnlinkProcessor,
            AppleRevokeProcessor appleRevokeProcessor,
            MeetingCoverCleanupProcessor coverCleanupProcessor
    ) {
        this.withdrawalService = withdrawalService;
        this.kakaoUnlinkProcessor = kakaoUnlinkProcessor;
        this.appleRevokeProcessor = appleRevokeProcessor;
        this.coverCleanupProcessor = coverCleanupProcessor;
    }

    /** Returns only after the local withdrawal transaction has committed. */
    public boolean withdraw(Long userId) {
        MemberWithdrawalService.WithdrawalResult result = withdrawalService.withdraw(userId);
        coverCleanupProcessor.process(result.coverCleanupTaskIds());
        MemberWithdrawalService.WithdrawalTask task = result.socialTask();
        if (task == null) return true;
        return switch (task.provider()) {
            case KAKAO -> kakaoUnlinkProcessor.process(task.taskId());
            case APPLE -> appleRevokeProcessor.process(task.taskId());
        };
    }
}
