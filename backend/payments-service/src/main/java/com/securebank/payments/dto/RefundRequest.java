package com.securebank.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RefundRequest(
  @NotBlank(message = "Authenticator code is required")
  @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit authenticator code")
  String totpCode
) {}
