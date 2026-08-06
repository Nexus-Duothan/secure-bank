package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/** The authenticator code that applies a staged password change. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeConfirmRequest {

  @NotBlank(message = "TOTP code is required")
  @Pattern(regexp = "\\d{6}", message = "TOTP code must be 6 digits")
  private String totpCode;
}
