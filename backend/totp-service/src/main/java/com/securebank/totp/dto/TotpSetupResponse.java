package com.securebank.totp.dto;

import java.util.List;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotpSetupResponse {

  private UUID userId;
  private String secretKey;
  private String otpauthUrl;
  private String qrCodeBase64;
  private List<String> scratchCodes;
}
