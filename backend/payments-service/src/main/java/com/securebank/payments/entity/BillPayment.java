package com.securebank.payments.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "bill_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPayment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "payer_user_id", nullable = false)
  private UUID payerUserId;

  @Column(name = "from_account_id", nullable = false, length = 64)
  private String fromAccountId;

  @Column(name = "biller_category", nullable = false, length = 50)
  private String billerCategory;

  @Column(name = "biller_name", nullable = false, length = 150)
  private String billerName;

  @Column(name = "reference_number", nullable = false, length = 100)
  private String referenceNumber;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Builder.Default
  @Column(nullable = false, length = 3)
  private String currency = "LKR";

  @Column(nullable = false, length = 20)
  private String status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
  }
}
