package com.securebank.payments.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {

  private UUID paymentId;
  private UUID payerUserId;
  private UUID merchantId;
  private BigDecimal amount;
  private String currency;
  private String referenceNumber;
  private Instant occurredAt;
}
