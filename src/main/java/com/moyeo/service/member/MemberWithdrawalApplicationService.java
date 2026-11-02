package com.moyeo.service.member;

import org.springframework.stereotype.Service;

@Service
public class MemberWithdrawalApplicationService {
    private final MemberWithdrawalService withdrawalService;
    private final KakaoUnlinkProcessor kakaoUnlinkProcessor;

    public MemberWithdrawalApplicationService(
            MemberWithdrawalService withdrawalService,
            KakaoUnlinkProcessor kakaoUnlinkProcessor
    ) {
        this.withdrawalService = withdrawalService;
        this.kakaoUnlinkProcessor = kakaoUnlinkProcessor;
    }

    /** Returns only after the local withdrawal transaction has committed. */
    public boolean withdraw(Long userId) {
        Long taskId = withdrawalService.withdraw(userId);
        return taskId == null || kakaoUnlinkProcessor.process(taskId);
    }
}
