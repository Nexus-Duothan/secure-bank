package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationOtpVerifyRequest {

  @NotNull(message = "User ID is required")
  private UUID userId;

  @NotBlank(message = "Verification code is required")
  private String code;
}
