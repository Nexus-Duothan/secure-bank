package com.securebank.accounts;

import java.math.BigDecimal;

/** Confirmation that a movement was posted, with the balance it left behind. */
public record LedgerEntryResponse(
  String accountId,
  String transactionId,
  String journalId,
  BigDecimal newBalance
) {}
