-- Adding a payee is now confirmed with the caller's authenticator app (TOTP) instead of an SMS
-- one-time code, so the service no longer generates or stores a code digest.
ALTER TABLE pending_payee_additions DROP COLUMN IF EXISTS otp_hash;
