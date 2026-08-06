-- Staged profile / admin changes are now confirmed with the customer's authenticator app (TOTP)
-- instead of an SMS one-time code, so the service no longer generates or stores a code digest.
ALTER TABLE pending_user_changes DROP COLUMN IF EXISTS otp_hash;
