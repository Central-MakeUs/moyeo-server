-- Back up the target DB before applying. Additive migration; apply before deploying the app.
CREATE TABLE IF NOT EXISTS kakao_unlink_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider_user_id VARCHAR(191) NOT NULL,
    attempt_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_attempted_at DATETIME(6) NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_kakao_unlink_tasks_user UNIQUE (user_id),
    CONSTRAINT uk_kakao_unlink_tasks_provider_user UNIQUE (provider_user_id),
    INDEX idx_kakao_unlink_tasks_due (next_attempt_at, id)
);
-- Rollback: restore the prior app first. Retain this table while tasks remain;
-- dropping pending tasks loses external cleanup and must not be an automatic rollback.
