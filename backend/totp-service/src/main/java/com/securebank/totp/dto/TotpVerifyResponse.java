package com.securebank.totp.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpVerifyResponse {

  private boolean valid;
  private String message;
  private boolean usedScratchCode;
}
