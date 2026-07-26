-- Back up the production database before applying this migration.
-- Apply once before deploying re-login-free Apple account withdrawal.

ALTER TABLE social_accounts
    ADD COLUMN provider_refresh_token_ciphertext VARCHAR(2048) NULL
    COMMENT '서버 암호화 키로 암호화한 제공자 refresh token. 현재 Apple만 저장'
    AFTER email;
