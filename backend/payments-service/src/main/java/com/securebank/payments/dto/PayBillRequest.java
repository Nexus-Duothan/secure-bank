package com.securebank.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PayBillRequest(
  @NotBlank String billerCategory,
  @NotBlank String billerName,
  @NotBlank String referenceNumber,
  @NotNull @DecimalMin("0.01") BigDecimal amount,
  @NotBlank String fromAccountId,
  @NotBlank(message = "Authenticator code is required")
  @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit authenticator code")
  String totpCode
) {}
