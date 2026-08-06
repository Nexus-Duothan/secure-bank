package com.securebank.accounts;

import java.math.BigDecimal;

public record RefundLedgerResponse(
  String merchantAccountId,
  BigDecimal merchantBalance,
  String customerAccountId,
  BigDecimal customerBalance
) {}
