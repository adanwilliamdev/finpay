-- Additional indexes for performance
CREATE INDEX idx_transactions_source_status ON transactions(source_wallet_id, status);
CREATE INDEX idx_transactions_dest_status ON transactions(destination_wallet_id, status);
CREATE INDEX idx_transactions_type_status ON transactions(type, status);
CREATE INDEX idx_transactions_wallet_created ON transactions(source_wallet_id, created_at DESC);

-- Composite index for audit logs
CREATE INDEX idx_audit_logs_user_action ON audit_logs(user_id, action);
CREATE INDEX idx_audit_logs_entity_created ON audit_logs(entity_type, created_at DESC);