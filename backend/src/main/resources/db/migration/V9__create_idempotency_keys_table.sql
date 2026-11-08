-- Idempotency-Key support for deposits, withdrawals and transfers.
-- A key is scoped to the user that sent it. The row is written in the SAME database
-- transaction that moves the money, so "money moved" and "key remembered" are atomic:
-- if two requests with the same key race, the unique constraint makes the loser roll back.
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    transaction_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_idempotency_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys(created_at);
