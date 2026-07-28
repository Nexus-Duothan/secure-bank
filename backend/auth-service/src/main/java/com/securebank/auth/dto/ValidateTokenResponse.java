package com.securebank.auth.dto;

import com.securebank.auth.enums.Role;
import com.securebank.auth.enums.UserStatus;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTokenResponse {

  private boolean valid;
  private UUID userId;
  private String username;
  private Role role;
  private UserStatus status;
}
