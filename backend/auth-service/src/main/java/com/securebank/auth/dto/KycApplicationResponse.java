package com.securebank.auth.dto;

import com.securebank.auth.enums.DocumentType;
import com.securebank.auth.enums.KycStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycApplicationResponse {

  private UUID applicationId;
  private UUID userId;
  private DocumentType documentType;
  private String documentNumber;
  private KycStatus status;
  private String rejectionReason;
  private Instant submittedAt;
  private Instant reviewedAt;
  private String reviewedBy;
}
