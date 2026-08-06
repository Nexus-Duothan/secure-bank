package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendOtpRequest {

  @NotBlank(message = "Pre-authentication token or user reference is required")
  private String preAuthToken;

  private String usernameOrEmail;
}
