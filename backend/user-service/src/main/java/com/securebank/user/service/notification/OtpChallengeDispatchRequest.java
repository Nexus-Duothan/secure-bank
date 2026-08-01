package com.securebank.user.service.notification;

import java.time.Instant;
import java.util.UUID;

public record OtpChallengeDispatchRequest(
  UUID userId,
  String fullName,
  String email,
  String phoneNumber,
  boolean emailEnabled,
  boolean smsEnabled,
  String changeType,
  String otpCode,
  Instant expiresAt
) {}
