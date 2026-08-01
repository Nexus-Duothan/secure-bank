package com.securebank.accounts;

import java.math.BigDecimal;

public record TransactionResponse(
  String id,
  String merchant,
  String category,
  String date,
  BigDecimal amount,
  boolean verified
) {}
