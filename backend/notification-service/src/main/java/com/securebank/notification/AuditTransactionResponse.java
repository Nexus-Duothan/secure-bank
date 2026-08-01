package com.securebank.notification;

import java.math.BigDecimal;

public record AuditTransactionResponse(
  String id,
  String merchant,
  String category,
  String location,
  BigDecimal amount,
  String currency,
  String timestamp,
  String dateGroupLabel,
  String journalId,
  boolean flagged
) {}
