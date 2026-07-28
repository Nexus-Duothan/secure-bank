package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmRequest {

  @NotBlank(message = "Reset token is required")
  private String token;

  @NotBlank(message = "New password is required")
  @Size(min = 8, message = "Password must be at least 8 characters long")
  private String newPassword;

  @NotBlank(message = "TOTP code is required for MFA-protected reset")
  private String totpCode;
}
