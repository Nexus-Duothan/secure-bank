package com.securebank.auth.dto;

import com.securebank.auth.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerKycReviewRequest {

  @NotNull(message = "Review action status (APPROVED or REJECTED) is required")
  private KycStatus action;

  private String rejectionReason;
}
