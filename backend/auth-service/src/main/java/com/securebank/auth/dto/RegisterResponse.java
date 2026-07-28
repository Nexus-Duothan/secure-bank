package com.securebank.auth.dto;

import com.securebank.auth.enums.Role;
import com.securebank.auth.enums.UserStatus;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

  private UUID userId;
  private String username;
  private String email;
  private Role role;
  private UserStatus status;
  private String message;
}
