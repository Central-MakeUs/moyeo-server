-- Back up before applying to an existing database. MySQL 8.4.
-- Apply once; if the named constraint exists, verify its definition and ENFORCED status instead.
-- Preflight must be zero. Never fill missing credentials with placeholders; repair affected accounts first.
SELECT COUNT(*) AS invalid_apple_token_rows
FROM social_accounts
WHERE provider = 'APPLE'
  AND (provider_refresh_token_ciphertext IS NULL
       OR CHAR_LENGTH(TRIM(provider_refresh_token_ciphertext)) = 0);

ALTER TABLE social_accounts
    ADD CONSTRAINT ck_social_accounts_apple_refresh_token
    CHECK (provider <> 'APPLE'
           OR (provider_refresh_token_ciphertext IS NOT NULL
               AND CHAR_LENGTH(TRIM(provider_refresh_token_ciphertext)) > 0)) ENFORCED;

-- Verify after applying:
SELECT CONSTRAINT_NAME, ENFORCED
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'social_accounts'
  AND CONSTRAINT_NAME = 'ck_social_accounts_apple_refresh_token';
-- No automatic rollback: the additive constraint is compatible with the current deployed login flow.
