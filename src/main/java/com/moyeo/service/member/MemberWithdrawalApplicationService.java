package com.moyeo.service.member;

import org.springframework.stereotype.Service;

@Service
public class MemberWithdrawalApplicationService {
    private final MemberWithdrawalService withdrawalService;
    private final KakaoUnlinkProcessor kakaoUnlinkProcessor;
    private final AppleRevokeProcessor appleRevokeProcessor;

    public MemberWithdrawalApplicationService(
            MemberWithdrawalService withdrawalService,
            KakaoUnlinkProcessor kakaoUnlinkProcessor,
            AppleRevokeProcessor appleRevokeProcessor
    ) {
        this.withdrawalService = withdrawalService;
        this.kakaoUnlinkProcessor = kakaoUnlinkProcessor;
        this.appleRevokeProcessor = appleRevokeProcessor;
    }

    /** Returns only after the local withdrawal transaction has committed. */
    public boolean withdraw(Long userId) {
        MemberWithdrawalService.WithdrawalTask task = withdrawalService.withdraw(userId);
        if (task == null) return true;
        return switch (task.provider()) {
            case KAKAO -> kakaoUnlinkProcessor.process(task.taskId());
            case APPLE -> appleRevokeProcessor.process(task.taskId());
        };
    }
}
