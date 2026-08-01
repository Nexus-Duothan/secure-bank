package com.securebank.payments.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHeldEvent {

  private UUID paymentId;
  private UUID payerUserId;
  private UUID merchantId;
  private BigDecimal amount;
  private String reason;
  private Instant occurredAt;
}
