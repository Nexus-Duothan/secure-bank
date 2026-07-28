package com.securebank.auth.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreAuthResponse {

  private String preAuthToken;
  private boolean mfaRequired;
  private String message;
}
