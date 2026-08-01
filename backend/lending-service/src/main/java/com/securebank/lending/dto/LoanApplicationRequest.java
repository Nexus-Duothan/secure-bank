package com.securebank.lending.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LoanApplicationRequest(
  @NotBlank String purpose,
  @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount,
  @Min(value = 1, message = "termMonths must be at least 1") int termMonths,
  @NotBlank String linkedAccountId
) {}
