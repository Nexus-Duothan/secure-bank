package com.securebank.transfer.dto;

import com.securebank.transfer.enums.ScheduleFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateScheduledTransferRequest(
  @NotBlank String fromAccountId,
  @NotBlank String toAccount,
  @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than 0") BigDecimal amount,
  String note,
  @NotNull ScheduleFrequency frequency,
  @NotNull Instant startAt,
  Instant endDate
) {}
