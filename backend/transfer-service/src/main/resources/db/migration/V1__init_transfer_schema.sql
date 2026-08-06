CREATE TABLE IF NOT EXISTS transfers (
  id UUID PRIMARY KEY,
  initiated_by_user_id UUID NOT NULL,
  from_account_id VARCHAR(64) NOT NULL,
  to_account VARCHAR(64) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL,
  fee NUMERIC(19, 2) NOT NULL DEFAULT 0,
  currency VARCHAR(3) NOT NULL,
  note VARCHAR(280),
  status VARCHAR(30) NOT NULL,
  failure_reason VARCHAR(280),
  idempotency_key VARCHAR(100),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP WITH TIME ZONE
);

-- A retried quote with the same caller-supplied key must resolve to the same transfer
-- rather than creating a duplicate. No WHERE predicate is needed to exempt NULL keys: both
-- Postgres and H2 already treat NULL as distinct from NULL in a unique index, so callers who
-- skip the idempotency key are never deduped against each other. A partial index here also
-- isn't portable to H2's PostgreSQL-compatibility mode, which rejects the WHERE clause syntax.
CREATE UNIQUE INDEX IF NOT EXISTS uq_transfers_user_idempotency_key
  ON transfers(initiated_by_user_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_transfers_from_account_id ON transfers(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transfers_initiated_by_user_id ON transfers(initiated_by_user_id);

CREATE TABLE IF NOT EXISTS transfer_daily_usage (
  account_id VARCHAR(64) NOT NULL,
  usage_date DATE NOT NULL,
  total_amount NUMERIC(19, 2) NOT NULL DEFAULT 0,
  PRIMARY KEY (account_id, usage_date)
);
