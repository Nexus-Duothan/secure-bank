-- TOTP Service Initial Database Schema (V1)
CREATE TABLE IF NOT EXISTS user_totp_secrets (
  user_id UUID PRIMARY KEY,
  secret_key VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP
  WITH
    TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
  WITH
    TIME ZONE
);

CREATE TABLE IF NOT EXISTS user_totp_scratch_codes (
  user_id UUID NOT NULL REFERENCES user_totp_secrets (user_id) ON DELETE CASCADE,
  scratch_code VARCHAR(20) NOT NULL
);

-- Indexes for fast query performance
CREATE INDEX IF NOT EXISTS idx_user_totp_secrets_enabled ON user_totp_secrets (user_id, enabled);

CREATE INDEX IF NOT EXISTS idx_user_totp_scratch_codes_user_id ON user_totp_scratch_codes (user_id);
