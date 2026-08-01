package com.securebank.payments.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponse {

  private UUID id;
  private String merchantCode;
  private String businessName;
  private String category;
  private boolean active;
  private Instant createdAt;
}
