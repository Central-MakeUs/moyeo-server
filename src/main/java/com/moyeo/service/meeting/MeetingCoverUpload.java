package com.moyeo.service.meeting;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Tracks a confirmed rollback; an unknown commit result must never trigger blind deletion. */
public final class MeetingCoverUpload {
    private final String objectKey;
    private int completionStatus = TransactionSynchronization.STATUS_UNKNOWN;

    public MeetingCoverUpload(String objectKey) {
        this.objectKey = objectKey;
    }

    public String objectKey() { return objectKey; }

    void trackTransactionCompletion() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                completionStatus = status;
            }
        });
    }

    boolean wasRolledBack() {
        return completionStatus == TransactionSynchronization.STATUS_ROLLED_BACK;
    }
}
