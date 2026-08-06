package com.securebank.lending.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published so notification-service can alert the borrower ahead of an installment's due date. */
public record RepaymentReminderEvent(
  UUID loanId,
  UUID borrowerUserId,
  UUID installmentId,
  int installmentNumber,
  BigDecimal amount,
  Instant dueDate,
  Instant occurredAt
) {}
