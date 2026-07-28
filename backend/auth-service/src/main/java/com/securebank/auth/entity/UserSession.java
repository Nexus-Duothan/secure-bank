package com.securebank.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
  name = "user_sessions",
  indexes = {
    @Index(name = "idx_sessions_user_id", columnList = "user_id"),
    @Index(name = "idx_sessions_refresh_token", columnList = "refresh_token", unique = true),
  }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "session_token_hash", nullable = false)
  private String sessionTokenHash;

  @Column(name = "refresh_token", nullable = false, unique = true)
  private String refreshToken;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 255)
  private String userAgent;

  @Column(name = "device_info", length = 100)
  private String deviceInfo;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "last_active_at", nullable = false)
  private Instant lastActiveAt;

  @Builder.Default
  @Column(nullable = false)
  private boolean revoked = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
    this.lastActiveAt = Instant.now();
  }
}
