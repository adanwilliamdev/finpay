-- V1/V2/V3 already declare UNIQUE on these columns (which creates a unique index),
-- and V5 added the very same UNIQUE constraints a second time. Several plain indexes
-- on the same columns were also created on top of the unique ones. Every redundant
-- index only costs write throughput and disk, so drop the duplicates.
-- (Done in a new migration instead of editing V1-V5, so Flyway checksums of
-- already-migrated databases stay valid.)

-- Duplicate UNIQUE constraints added by V5 (originals: users_email_key,
-- users_document_number_key, wallets_wallet_number_key)
ALTER TABLE users   DROP CONSTRAINT IF EXISTS uk_users_email;
ALTER TABLE users   DROP CONSTRAINT IF EXISTS uk_users_document;
ALTER TABLE wallets DROP CONSTRAINT IF EXISTS uk_wallets_wallet_number;

-- Plain indexes that duplicate the index behind a UNIQUE constraint
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_users_document_number;
DROP INDEX IF EXISTS idx_wallets_wallet_number;
DROP INDEX IF EXISTS idx_transactions_transaction_id;
