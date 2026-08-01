package com.securebank.lending.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanDisbursedEvent(
  UUID loanId,
  UUID borrowerUserId,
  BigDecimal principal,
  String currency,
  int termMonths,
  Instant occurredAt
) {}
