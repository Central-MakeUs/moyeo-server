-- Apply once before deploying member feedback to an existing production database.
-- Back up the database and verify the target schema before execution.
CREATE TABLE feedbacks (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '피드백 ID',
    user_id BIGINT NOT NULL COMMENT '피드백을 제출한 서비스 사용자 ID',
    content VARCHAR(1000) NOT NULL COMMENT '사용자가 제출한 피드백 내용',
    created_at DATETIME NOT NULL COMMENT '피드백 제출 일시',
    PRIMARY KEY (id),
    CONSTRAINT fk_feedbacks_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_feedbacks_user_created (user_id, created_at, id)
) COMMENT='사용자 피드백';
