package com.securebank.transfer.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "payees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payee {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_user_id", nullable = false)
  private UUID ownerUserId;

  @Column(nullable = false, length = 80)
  private String nickname;

  @Column(name = "account_reference", nullable = false, length = 64)
  private String accountReference;

  /** Large transfers to this payee are rejected until this instant (FR-16). */
  @Column(name = "cooling_off_until", nullable = false)
  private Instant coolingOffUntil;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }

  public boolean isCoolingOff(Instant now) {
    return now.isBefore(coolingOffUntil);
  }
}
