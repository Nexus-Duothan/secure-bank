package com.securebank.payments.dto;

import com.securebank.payments.entity.BillPayment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BillPaymentResponse(
  UUID id,
  String status,
  String billerName,
  String referenceNumber,
  BigDecimal amount,
  String currency,
  Instant createdAt
) {
  public static BillPaymentResponse from(BillPayment payment) {
    return new BillPaymentResponse(
      payment.getId(),
      payment.getStatus(),
      payment.getBillerName(),
      payment.getReferenceNumber(),
      payment.getAmount(),
      payment.getCurrency(),
      payment.getCreatedAt()
    );
  }
}
