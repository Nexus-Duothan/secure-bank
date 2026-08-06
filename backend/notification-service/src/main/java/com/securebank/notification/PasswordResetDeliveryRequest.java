package com.securebank.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record PasswordResetDeliveryRequest(
  @NotNull UUID userId,
  @NotBlank String fullName,
  @NotBlank @Email String email,
  @NotBlank String resetUrl,
  @NotNull Instant expiresAt
) {}
