package com.securebank.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A debit or credit card issued against an account. */
@Entity
@Table(name = "bank_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankCardEntity {

  @Id
  @Column(length = 60)
  private String id;

  @Column(name = "account_id", length = 60)
  private String accountId;

  /** Null while the card is issued but not yet claimed by a customer. */
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "card_type", nullable = false, length = 10)
  private String cardType;

  @Column(name = "product_name", nullable = false, length = 80)
  private String productName;

  @Column(name = "card_number", nullable = false, unique = true, length = 19)
  private String cardNumber;

  @Column(name = "masked_number", nullable = false, length = 30)
  private String maskedNumber;

  @Column(name = "cardholder_name", nullable = false, length = 120)
  private String cardholderName;

  @Column(name = "expiry_date", nullable = false, length = 5)
  private String expiryDate;

  @Column(name = "holder_national_id", length = 40)
  private String holderNationalId;

  @Column(nullable = false, length = 20)
  private String scheme;

  @Column(nullable = false, length = 30)
  private String status;

  @Builder.Default
  @Column(name = "joint_account_card", nullable = false)
  private boolean jointAccountCard = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
