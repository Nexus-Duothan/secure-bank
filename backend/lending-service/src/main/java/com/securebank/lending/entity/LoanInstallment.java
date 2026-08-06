package com.securebank.lending.entity;

import com.securebank.lending.enums.InstallmentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * One row per scheduled installment, computed and persisted in full at disbursement time
 * (reducing-balance/EMI amortization) rather than recomputed on the fly — so schedule display
 * and due-date collection are both plain row lookups/updates.
 */
@Entity
@Table(name = "loan_installments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInstallment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "loan_id", nullable = false)
  private UUID loanId;

  @Column(name = "installment_number", nullable = false)
  private int installmentNumber;

  @Column(name = "due_date", nullable = false)
  private Instant dueDate;

  @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal principalAmount;

  @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal interestAmount;

  @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "remaining_balance_after", nullable = false, precision = 19, scale = 2)
  private BigDecimal remainingBalanceAfter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private InstallmentStatus status = InstallmentStatus.PENDING;

  @Column(name = "paid_at")
  private Instant paidAt;

  /** Failed collection attempts so far; the installment is marked OVERDUE past the configured max. */
  @Column(name = "failed_attempts", nullable = false)
  @Builder.Default
  private int failedAttempts = 0;

  @Column(name = "next_retry_at")
  private Instant nextRetryAt;

  /** So the reminder for a given installment fires at most once. */
  @Column(name = "reminder_sent_at")
  private Instant reminderSentAt;
}
