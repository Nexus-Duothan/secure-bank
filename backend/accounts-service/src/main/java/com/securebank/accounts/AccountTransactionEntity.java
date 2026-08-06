package com.securebank.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One posted movement on an account. Rows are only ever appended - the ledger is
 * the single source of truth behind every balance and activity list the API returns.
 */
@Entity
@Table(name = "account_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransactionEntity {

  @Id
  @Column(length = 60)
  private String id;

  @Column(name = "account_id", nullable = false, length = 60)
  private String accountId;

  @Column(nullable = false, length = 140)
  private String merchant;

  @Column(nullable = false, length = 60)
  private String category;

  @Column(name = "transaction_type", nullable = false, length = 40)
  private String transactionType;

  @Column(length = 120)
  private String location;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "balance_after", precision = 19, scale = 2)
  private BigDecimal balanceAfter;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "journal_id", nullable = false, length = 40)
  private String journalId;

  @Builder.Default
  @Column(nullable = false)
  private boolean flagged = false;

  /** Idempotency key from the service that posted the movement, if it supplied one. */
  @Column(length = 80)
  private String reference;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
