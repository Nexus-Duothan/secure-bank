package com.securebank.user.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "user_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_profile_id", nullable = false)
  private UserProfile userProfile;

  @Column(name = "device_name", nullable = false, length = 100)
  private String deviceName;

  @Column(name = "device_type", length = 60)
  private String deviceType;

  @Column(length = 80)
  private String browser;

  @Column(length = 100)
  private String location;

  @Builder.Default
  @Column(nullable = false)
  private boolean trusted = false;

  @Column(name = "last_verified_at")
  private Instant lastVerifiedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }
}
