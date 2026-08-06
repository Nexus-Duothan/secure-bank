package com.securebank.accounts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A customer account as it is actually stored in the ledger database. */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {

  @Id
  @Column(length = 60)
  private String id;

  /** Null while the account is issued but not yet claimed by a customer. */
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "holder_name", length = 120)
  private String holderName;

  @Column(name = "holder_national_id", length = 40)
  private String holderNationalId;

  @Column(name = "holder_address_line", length = 180)
  private String holderAddressLine;

  @Column(name = "holder_city", length = 80)
  private String holderCity;

  @Column(nullable = false, length = 120)
  private String nickname;

  @Column(name = "account_type", nullable = false, length = 20)
  private String accountType;

  @Column(name = "product_code", length = 40)
  private String productCode;

  @Column(name = "product_name", length = 120)
  private String productName;

  @Column(name = "account_number", nullable = false, unique = true, length = 30)
  private String accountNumber;

  @Builder.Default
  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance = BigDecimal.ZERO;

  @Builder.Default
  @Column(nullable = false, length = 3)
  private String currency = "LKR";

  @Column(name = "ifsc_code", nullable = false, length = 20)
  private String ifscCode;

  @Column(name = "opened_on", nullable = false)
  private LocalDate openedOn;

  @Column(name = "home_branch", nullable = false, length = 80)
  private String homeBranch;

  @Column(name = "ownership_label", nullable = false, length = 40)
  private String ownershipLabel;

  @Column(nullable = false, length = 40)
  private String status;

  @Builder.Default
  @Column(nullable = false)
  private boolean frozen = false;

  @Column(name = "freeze_reason", length = 180)
  private String freezeReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public String lastFourDigits() {
    return accountNumber == null || accountNumber.length() < 4
      ? "0000"
      : accountNumber.substring(accountNumber.length() - 4);
  }

  public void freeze(String reason) {
    this.frozen = true;
    this.freezeReason = reason == null || reason.isBlank() ? "Customer protection request" : reason;
    this.status = "Frozen - Protected";
  }

  public void unfreeze() {
    this.frozen = false;
    this.freezeReason = null;
    this.status = "Active - Verified";
  }
}
