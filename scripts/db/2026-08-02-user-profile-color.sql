-- Apply once before deploying profile-color selection to an existing production database.
-- Back up the database and verify the target schema before execution.
ALTER TABLE users
    ADD COLUMN profile_color VARCHAR(20) NOT NULL DEFAULT 'GRAY'
    COMMENT '회원 기본 프로필 색상: GRAY/RED/PURPLE/ORANGE'
    AFTER nickname;
