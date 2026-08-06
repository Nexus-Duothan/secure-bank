package com.securebank.user.client;

import java.util.UUID;

/** Checks a 6-digit authenticator (TOTP) code for a user against totp-service. */
public interface TotpClient {
  boolean verify(UUID userId, String code);
}
