package com.securebank.accounts;

import java.math.BigDecimal;

/** The slice of an account another core service needs before it moves money. */
public record AccountSnapshotResponse(
  String id,
  String accountNumber,
  BigDecimal balance,
  String currency,
  boolean frozen
) {}
