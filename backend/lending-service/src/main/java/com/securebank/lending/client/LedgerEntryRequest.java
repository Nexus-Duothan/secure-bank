package com.securebank.lending.client;

import java.math.BigDecimal;

/**
 * A movement lending asks accounts-service to post on the customer's account.
 *
 * @param reference the caller's idempotency key; posting the same reference twice on the same
 *     account is a no-op there, so a retried disbursement or collection cannot double up.
 */
public record LedgerEntryRequest(
  BigDecimal amount,
  String currency,
  String reference,
  String merchant,
  String category,
  String transactionType,
  String location
) {}
