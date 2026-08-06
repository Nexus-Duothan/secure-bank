package com.securebank.accounts;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * A movement another core service (transfers, payments, lending) asks the ledger to post.
 *
 * @param reference the caller's idempotency key; posting the same reference twice on the same
 *     account is a no-op, so a retry cannot double-charge the customer.
 */
public record LedgerEntryRequest(
  @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
  String currency,
  String reference,
  String merchant,
  String category,
  String transactionType,
  String location
) {}
