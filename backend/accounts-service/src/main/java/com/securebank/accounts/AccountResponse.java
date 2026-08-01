package com.securebank.accounts;

import java.math.BigDecimal;

public record AccountResponse(
  String id,
  String nickname,
  String accountType,
  String lastFourDigits,
  String accountNumber,
  BigDecimal balance,
  String currency,
  double monthlyChangePercent,
  String verifiedLabel,
  String status,
  boolean frozen,
  String freezeReason
) {}
