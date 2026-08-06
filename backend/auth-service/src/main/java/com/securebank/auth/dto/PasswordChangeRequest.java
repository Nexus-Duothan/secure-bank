package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * A signed-in customer changing their own password (FR-05). The current password proves it is
 * really them at the keyboard; the change itself is only applied after the authenticator code is
 * confirmed on the follow-up call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {

  @NotBlank(message = "Current password is required")
  private String currentPassword;

  @NotBlank(message = "New password is required")
  @Size(min = 8, message = "Password must be at least 8 characters long")
  private String newPassword;
}
