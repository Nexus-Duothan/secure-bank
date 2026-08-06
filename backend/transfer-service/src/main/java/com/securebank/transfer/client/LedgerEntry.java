package com.securebank.transfer.client;

import java.math.BigDecimal;

/**
 * A movement transfer-service asks the ledger to post.
 *
 * @param reference idempotency key; posting the same reference twice on one account is a no-op
 */
public record LedgerEntry(
  BigDecimal amount,
  String currency,
  String reference,
  String merchant,
  String category,
  String transactionType,
  String location
) {}
