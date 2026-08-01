package com.securebank.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferQuoteRequest(
  @NotBlank String fromAccountId,
  @NotBlank String toAccount,
  @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount,
  String note
) {}
