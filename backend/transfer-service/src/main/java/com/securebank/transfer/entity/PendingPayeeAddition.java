package com.securebank.transfer.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "pending_payee_additions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingPayeeAddition {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(nullable = false, length = 80)
  private String nickname;

  @Column(name = "account_reference", nullable = false, length = 64)
  private String accountReference;

  /** BCrypt digest of the one-time code. The code itself is never persisted (NFR-S4). */
  @Column(name = "otp_hash", nullable = false, length = 100)
  private String otpHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Builder.Default
  @Column(nullable = false)
  private boolean confirmed = false;

  /** Wrong codes submitted so far; the challenge is burned once the configured limit is hit. */
  @Builder.Default
  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }

  public boolean isExpired(Instant now) {
    return now.isAfter(expiresAt);
  }
}
