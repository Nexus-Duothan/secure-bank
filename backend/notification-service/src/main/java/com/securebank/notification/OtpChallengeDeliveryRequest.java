package com.securebank.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record OtpChallengeDeliveryRequest(
  @NotNull UUID userId,
  @NotBlank String fullName,
  String email,
  String phoneNumber,
  boolean emailEnabled,
  boolean smsEnabled,
  @NotBlank String changeType,
  @NotBlank String otpCode,
  @NotNull Instant expiresAt
) {}
