package com.securebank.payments.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPayRequest {

  @NotBlank(message = "QR payload is required")
  private String qrPayload;

  /** Optional override — used when the scanned code doesn't embed a suggestedAmount. */
  private BigDecimal amount;
}
