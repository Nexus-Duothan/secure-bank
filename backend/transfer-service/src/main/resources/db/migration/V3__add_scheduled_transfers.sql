CREATE TABLE IF NOT EXISTS scheduled_transfers (
  id UUID PRIMARY KEY,
  owner_user_id UUID NOT NULL,
  from_account_id VARCHAR(64) NOT NULL,
  to_account VARCHAR(64) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL,
  note VARCHAR(280),
  frequency VARCHAR(20) NOT NULL,
  next_run_at TIMESTAMP WITH TIME ZONE NOT NULL,
  end_date TIMESTAMP WITH TIME ZONE,
  status VARCHAR(20) NOT NULL,
  last_executed_at TIMESTAMP WITH TIME ZONE,
  last_execution_status VARCHAR(280),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_scheduled_transfers_owner_user_id ON scheduled_transfers(owner_user_id);

-- Drives the runner's due-schedule poll (status = ACTIVE, next_run_at <= now).
CREATE INDEX IF NOT EXISTS idx_scheduled_transfers_due ON scheduled_transfers(status, next_run_at);
