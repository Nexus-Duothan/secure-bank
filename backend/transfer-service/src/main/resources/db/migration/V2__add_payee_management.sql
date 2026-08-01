CREATE TABLE IF NOT EXISTS payees (
  id UUID PRIMARY KEY,
  owner_user_id UUID NOT NULL,
  nickname VARCHAR(80) NOT NULL,
  account_reference VARCHAR(64) NOT NULL,
  cooling_off_until TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- A caller can only save a given account as a payee once (FR-16). Kept as a plain
-- index so the script runs on H2 as well as PostgreSQL: H2 has no function-based
-- indexes. Case-insensitive matching is enforced in PayeeService, which checks
-- existsByOwnerUserIdAndAccountReferenceIgnoreCase before inserting.
CREATE UNIQUE INDEX IF NOT EXISTS uq_payees_owner_account
  ON payees(owner_user_id, account_reference);

CREATE INDEX IF NOT EXISTS idx_payees_owner_user_id ON payees(owner_user_id);

CREATE TABLE IF NOT EXISTS pending_payee_additions (
  id UUID PRIMARY KEY,
  owner_user_id UUID NOT NULL,
  nickname VARCHAR(80) NOT NULL,
  account_reference VARCHAR(64) NOT NULL,
  otp_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  failed_attempts INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_pending_payee_additions_owner_user_id
  ON pending_payee_additions(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_pending_payee_additions_expires_at
  ON pending_payee_additions(expires_at);
