package com.securebank.transfer.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferFailedEvent(
  UUID transferId,
  UUID initiatedByUserId,
  String fromAccountId,
  String toAccount,
  BigDecimal amount,
  String currency,
  String reason,
  Instant occurredAt
) {}
