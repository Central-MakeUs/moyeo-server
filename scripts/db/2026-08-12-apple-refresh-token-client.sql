ALTER TABLE social_accounts
    ADD COLUMN apple_refresh_token_client VARCHAR(20) NULL
        COMMENT 'Apple refresh token을 발급한 client: WEB 또는 NATIVE; 기존 NULL은 WEB으로 해석';
