package com.securebank.accounts;

import java.time.Instant;
import java.util.UUID;

public record OtpChallengeResponse(
  UUID changeRequestId,
  String type,
  String deliveryTarget,
  Instant expiresAt,
  String message,
  String demoCode
) {}
