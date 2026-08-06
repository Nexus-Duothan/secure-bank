package com.securebank.accounts;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One line of the bank-wide transaction journal (FR-30), as read by the admin audit view. It is
 * the stored ledger row - nothing here is derived from anything but {@code account_transactions}.
 */
public record JournalEntryResponse(
  String id,
  String accountId,
  String merchant,
  String category,
  String transactionType,
  String location,
  BigDecimal amount,
  String currency,
  Instant timestamp,
  String journalId,
  boolean flagged
) {}
