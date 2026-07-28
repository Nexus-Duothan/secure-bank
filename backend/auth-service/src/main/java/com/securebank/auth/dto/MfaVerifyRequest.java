package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyRequest {

  @NotBlank(message = "Pre-auth token is required")
  private String preAuthToken;

  @NotBlank(message = "TOTP code is required")
  private String totpCode;
}
