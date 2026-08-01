package com.securebank.transfer.dto;

import java.time.Instant;
import java.util.UUID;

public record PayeeChallengeResponse(
  UUID changeRequestId,
  Instant expiresAt,
  String message,
  String demoCode
) {}
