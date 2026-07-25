CREATE TABLE IF NOT EXISTS meeting_cover_cleanup_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_key VARCHAR(500) NOT NULL,
    attempt_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    last_attempted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_meeting_cover_cleanup_tasks_object_key UNIQUE (object_key)
);
