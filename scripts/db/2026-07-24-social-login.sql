-- Apply once before deploying social login to an existing production database.
-- Back up the database and verify the target schema before execution.
ALTER TABLE users
    MODIFY COLUMN nickname VARCHAR(30) NULL DEFAULT NULL
    COMMENT '사용자 기본 닉네임. null이면 소셜 가입 후 온보딩 미완료';
