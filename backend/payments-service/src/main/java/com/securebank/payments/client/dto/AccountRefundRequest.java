package com.securebank.payments.client.dto;

import java.math.BigDecimal;

public record AccountRefundRequest(
  String merchantAccountId,
  String customerUserId,
  BigDecimal amount,
  String currency,
  String reference,
  String merchant
) {}
