package com.securebank.totp.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "user_totp_secrets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTotpSecret {

  @Id
  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "secret_key", nullable = false)
  private String secretKey;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_totp_scratch_codes", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "scratch_code")
  @Builder.Default
  private List<String> scratchCodes = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
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
