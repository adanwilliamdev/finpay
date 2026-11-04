-- The wallet history query filters on source_wallet_id OR destination_wallet_id and
-- sorts by created_at DESC. V6 only covered the source side with (wallet, created_at);
-- add the symmetric index for the destination side so PostgreSQL can answer both
-- branches of the OR from an index (BitmapOr) instead of scanning the table.
CREATE INDEX IF NOT EXISTS idx_transactions_dest_created
    ON transactions(destination_wallet_id, created_at DESC);
