package com.securebank.auth.dto;

import com.securebank.auth.enums.Role;
import com.securebank.auth.enums.UserStatus;
import lombok.*;

/**
 * A change to what a user is allowed to do, pushed in by user-service once an administrator has
 * confirmed it. auth-service owns sign-in, so a role or status only takes real effect here: the
 * profile row user-service keeps is a mirror for display.
 *
 * <p>Either field may be null, meaning "leave that one alone".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialAccessUpdateRequest {

  private UserStatus status;
  private Role role;
}
