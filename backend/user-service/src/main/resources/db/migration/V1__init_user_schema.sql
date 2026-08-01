CREATE TABLE IF NOT EXISTS user_profiles (
  id UUID PRIMARY KEY,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  phone_number VARCHAR(30),
  address_line VARCHAR(180),
  city VARCHAR(80),
  country VARCHAR(80),
  language VARCHAR(40),
  role VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL,
  id_verified BOOLEAN NOT NULL DEFAULT TRUE,
  email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
  sms_notifications BOOLEAN NOT NULL DEFAULT TRUE,
  push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
  frozen BOOLEAN NOT NULL DEFAULT FALSE,
  freeze_reason VARCHAR(180),
  frozen_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_devices (
  id UUID PRIMARY KEY,
  user_profile_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(60),
  browser VARCHAR(80),
  location VARCHAR(100),
  trusted BOOLEAN NOT NULL DEFAULT FALSE,
  last_verified_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  revoked_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS pending_user_changes (
  id UUID PRIMARY KEY,
  user_profile_id UUID NOT NULL,
  type VARCHAR(50) NOT NULL,
  payload_json TEXT NOT NULL,
  otp_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  failed_attempts INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_created_at ON user_profiles(created_at);
CREATE INDEX IF NOT EXISTS idx_user_devices_profile_id ON user_devices(user_profile_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_active_profile_id ON user_devices(user_profile_id, revoked_at);
CREATE INDEX IF NOT EXISTS idx_pending_user_changes_profile_id ON pending_user_changes(user_profile_id);
CREATE INDEX IF NOT EXISTS idx_pending_user_changes_expires_at ON pending_user_changes(expires_at);
