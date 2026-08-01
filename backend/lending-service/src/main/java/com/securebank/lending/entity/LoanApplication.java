package com.securebank.lending.entity;

import com.securebank.lending.enums.ApplicationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "applicant_user_id", nullable = false)
  private UUID applicantUserId;

  @Column(nullable = false, length = 60)
  private String purpose;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "term_months", nullable = false)
  private int termMonths;

  @Column(name = "annual_interest_rate", nullable = false, precision = 6, scale = 3)
  private BigDecimal annualInterestRate;

  /** Account the disbursed principal is paid into and installments are later collected from. */
  @Column(name = "linked_account_id", nullable = false, length = 64)
  private String linkedAccountId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApplicationStatus status;

  @Column(name = "reviewed_by")
  private UUID reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "rejection_reason", length = 280)
  private String rejectionReason;

  /** Set once this application's loan has been created; null until DISBURSED. */
  @Column(name = "loan_id")
  private UUID loanId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }
}
