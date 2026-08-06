package com.securebank.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReviewRequest {

  @NotNull(message = "Approve decision is required")
  private Boolean approve;

  private String note;

  @NotBlank(message = "Authenticator code is required")
  @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit authenticator code")
  private String totpCode;
}
