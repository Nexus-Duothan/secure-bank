package com.securebank.lending.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published when an installment exhausts its retry policy without a successful collection. */
public record RepaymentOverdueEvent(
  UUID loanId,
  UUID borrowerUserId,
  UUID installmentId,
  int installmentNumber,
  BigDecimal amount,
  Instant dueDate,
  Instant occurredAt
) {}
