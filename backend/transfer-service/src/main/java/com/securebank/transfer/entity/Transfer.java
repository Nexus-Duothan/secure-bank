package com.securebank.transfer.entity;

import com.securebank.transfer.enums.TransferStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "initiated_by_user_id", nullable = false)
  private UUID initiatedByUserId;

  @Column(name = "from_account_id", nullable = false, length = 64)
  private String fromAccountId;

  @Column(name = "to_account", nullable = false, length = 64)
  private String toAccount;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Builder.Default
  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal fee = BigDecimal.ZERO;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(length = 280)
  private String note;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TransferStatus status;

  @Column(name = "failure_reason", length = 280)
  private String failureReason;

  /** Caller-supplied dedupe key for the quote call; null when the caller didn't send one. */
  @Column(name = "idempotency_key", length = 100)
  private String idempotencyKey;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }

  public BigDecimal totalDebit() {
    return amount.add(fee);
  }
}
