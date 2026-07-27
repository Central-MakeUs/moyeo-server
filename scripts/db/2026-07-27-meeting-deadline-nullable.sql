-- Apply once before deploying no-deadline meeting creation to an existing production database.
-- Back up the database and verify the target schema before execution.
ALTER TABLE meetings
    MODIFY COLUMN deadline_at DATETIME(6) NULL
    COMMENT '모임 참여/응답 마감 일시. NULL이면 마감 없음';
