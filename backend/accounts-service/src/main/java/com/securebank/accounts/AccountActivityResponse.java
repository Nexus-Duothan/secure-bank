package com.securebank.accounts;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountActivityResponse(
  String id,
  String merchant,
  String category,
  String transactionType,
  String location,
  BigDecimal amount,
  String currency,
  Instant timestamp,
  String dateGroupLabel,
  String journalId,
  boolean flagged
) {}
