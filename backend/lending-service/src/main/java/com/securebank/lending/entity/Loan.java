package com.securebank.lending.entity;

import com.securebank.lending.enums.LoanStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "borrower_user_id", nullable = false)
  private UUID borrowerUserId;

  @Column(nullable = false, length = 60)
  private String purpose;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal principal;

  @Column(name = "annual_interest_rate", nullable = false, precision = 6, scale = 3)
  private BigDecimal annualInterestRate;

  @Column(name = "term_months", nullable = false)
  private int termMonths;

  @Column(nullable = false, length = 3)
  @Builder.Default
  private String currency = "LKR";

  @Column(name = "linked_account_id", nullable = false, length = 64)
  private String linkedAccountId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private LoanStatus status = LoanStatus.ACTIVE;

  @Column(name = "autopay_enabled", nullable = false)
  @Builder.Default
  private boolean autopayEnabled = true;

  @Column(name = "disbursed_at", nullable = false)
  private Instant disbursedAt;

  @PrePersist
  protected void onCreate() {
    if (this.disbursedAt == null) {
      this.disbursedAt = Instant.now();
    }
  }
}
