package com.securebank.user.entity;

import com.securebank.user.enums.ChangeRequestType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "pending_user_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingUserChange {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_profile_id", nullable = false)
  private UUID userProfileId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ChangeRequestType type;

  @Column(name = "payload_json", nullable = false, columnDefinition = "text")
  private String payloadJson;

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
