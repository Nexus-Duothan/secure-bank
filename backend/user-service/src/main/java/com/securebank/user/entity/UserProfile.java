package com.securebank.user.entity;

import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

  /**
   * Always the id auth-service holds for this customer, which is also the JWT subject the gateway
   * forwards as {@code X-User-Id}. It is assigned, never generated: a profile with an id of its own
   * would be unreachable, because every lookup here is by the caller's authenticated id.
   */
  @Id
  private UUID id;

  @Column(name = "full_name", nullable = false, length = 120)
  private String fullName;

  @Column(nullable = false, unique = true, length = 120)
  private String email;

  @Column(name = "phone_number", length = 30)
  private String phoneNumber;

  @Column(name = "address_line", length = 180)
  private String addressLine;

  @Column(length = 80)
  private String city;

  @Column(length = 80)
  private String country;

  @Column(length = 40)
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserStatus status;

  @Builder.Default
  @Column(name = "id_verified", nullable = false)
  private boolean idVerified = true;

  @Builder.Default
  @Column(name = "email_notifications", nullable = false)
  private boolean emailNotifications = true;

  @Builder.Default
  @Column(name = "sms_notifications", nullable = false)
  private boolean smsNotifications = true;

  @Builder.Default
  @Column(name = "push_notifications", nullable = false)
  private boolean pushNotifications = true;

  @Builder.Default
  @Column(nullable = false)
  private boolean frozen = false;

  @Column(name = "freeze_reason", length = 180)
  private String freezeReason;

  @Column(name = "frozen_at")
  private Instant frozenAt;

  @Builder.Default
  @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserDevice> devices = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public void addDevice(UserDevice device) {
    devices.add(device);
    device.setUserProfile(this);
  }

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
