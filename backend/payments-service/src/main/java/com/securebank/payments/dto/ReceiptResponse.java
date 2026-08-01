package com.securebank.payments.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

  private UUID paymentId;
  private String referenceNumber;
  private String merchantName;
  private BigDecimal amount;
  private String currency;
  private Instant issuedAt;
}
