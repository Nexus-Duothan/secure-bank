package com.securebank.payments.dto;

import java.math.BigDecimal;
import lombok.*;

/**
 * Decoded contents of a SecureBank merchant QR payload — a base64-encoded JSON object of
 * the form {"merchantCode":"MCH-1029","suggestedAmount":1500.00,"currency":"LKR"}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrPaymentDetails {

  private String merchantCode;
  private BigDecimal suggestedAmount;
  private String currency;
}
