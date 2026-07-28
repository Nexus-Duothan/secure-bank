package com.securebank.totp.dto;

import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpStatusResponse {

  private UUID userId;
  private boolean enabled;
  private boolean setupInitiated;
}
