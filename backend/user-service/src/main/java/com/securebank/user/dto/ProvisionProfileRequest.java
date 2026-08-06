package com.securebank.user.dto;

import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * The profile auth-service asks us to create when a customer registers, so the account they just
 * made has a real profile row from the first sign-in instead of nothing to read.
 */
public record ProvisionProfileRequest(
  @NotNull UUID id,
  @NotBlank String fullName,
  @NotBlank @Email String email,
  String phoneNumber,
  @NotNull Role role,
  @NotNull UserStatus status
) {}
