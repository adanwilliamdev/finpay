-- Add check constraints
ALTER TABLE wallets ADD CONSTRAINT chk_wallets_balance
    CHECK (balance >= 0);

ALTER TABLE transactions ADD CONSTRAINT chk_transactions_amount
    CHECK (amount > 0);

-- Add composite unique constraints
ALTER TABLE users ADD CONSTRAINT uk_users_email
    UNIQUE (email);

ALTER TABLE users ADD CONSTRAINT uk_users_document
    UNIQUE (document_number);

ALTER TABLE wallets ADD CONSTRAINT uk_wallets_wallet_number
    UNIQUE (wallet_number);