package com.securebank.totp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpEnableRequest {

  @NotNull(message = "User ID is required")
  private UUID userId;

  @NotBlank(message = "6-digit TOTP code is required")
  private String totpCode;
}
