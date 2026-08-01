package com.securebank.payments.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
  name = "merchants",
  indexes = {
    @Index(name = "idx_merchants_merchant_code", columnList = "merchant_code", unique = true),
    @Index(name = "idx_merchants_merchant_user_id", columnList = "merchant_user_id"),
  }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "merchant_code", nullable = false, unique = true, length = 30)
  private String merchantCode;

  @Column(name = "business_name", nullable = false, length = 150)
  private String businessName;

  @Column(length = 50)
  private String category;

  @Column(name = "settlement_account_id", nullable = false, length = 50)
  private String settlementAccountId;

  @Column(name = "merchant_user_id")
  private UUID merchantUserId;

  @Builder.Default
  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
