package com.securebank.auth.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * A change staged behind the authenticator step, in the same shape the other services use so the
 * one verification screen in the app can drive every flow.
 *
 * @param demoCode always null: no code is generated or delivered by the backend, the customer
 *     reads the current one from their authenticator app.
 */
public record OtpChallengeResponse(
  UUID changeRequestId,
  String type,
  String deliveryTarget,
  Instant expiresAt,
  String message,
  String demoCode
) {}
