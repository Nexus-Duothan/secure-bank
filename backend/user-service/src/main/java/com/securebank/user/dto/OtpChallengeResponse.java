package com.securebank.user.dto;

import com.securebank.user.enums.ChangeRequestType;
import java.time.Instant;
import java.util.UUID;

public record OtpChallengeResponse(
  UUID changeRequestId,
  ChangeRequestType type,
  String deliveryTarget,
  Instant expiresAt,
  String message,
  String demoCode
) {}
