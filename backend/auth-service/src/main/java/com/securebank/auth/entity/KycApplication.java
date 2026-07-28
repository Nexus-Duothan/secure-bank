package com.securebank.auth.entity;

import com.securebank.auth.enums.DocumentType;
import com.securebank.auth.enums.KycStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "kyc_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycApplication {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "document_type", nullable = false, length = 30)
  private DocumentType documentType;

  @Column(name = "document_number", nullable = false, length = 50)
  private String documentNumber;

  @Column(name = "document_payload", columnDefinition = "TEXT")
  private String documentPayload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private KycStatus status;

  @Column(name = "rejection_reason", length = 255)
  private String rejectionReason;

  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "reviewed_by", length = 50)
  private String reviewedBy;

  @PrePersist
  protected void onCreate() {
    this.submittedAt = Instant.now();
  }
}
