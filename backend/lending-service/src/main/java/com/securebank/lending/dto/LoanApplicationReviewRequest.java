package com.securebank.lending.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LoanApplicationReviewRequest(
  @NotNull Boolean approve,
  String note,
  @NotBlank(message = "Authenticator code is required")
  @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit authenticator code")
  String totpCode
) {}
