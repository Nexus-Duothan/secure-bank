package com.securebank.auth.dto;

import com.securebank.auth.enums.KycStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerKycReviewRequest {

  @NotNull(message = "Review action status (APPROVED or REJECTED) is required")
  private KycStatus action;

  private String rejectionReason;

  @NotBlank(message = "Authenticator code is required")
  @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit authenticator code")
  private String totpCode;
}
