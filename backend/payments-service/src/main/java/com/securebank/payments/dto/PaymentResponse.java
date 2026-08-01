package com.securebank.payments.dto;

import com.securebank.payments.enums.PaymentChannel;
import com.securebank.payments.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

  private UUID id;
  private UUID payerUserId;
  private String merchantCode;
  private String merchantName;
  private BigDecimal amount;
  private String currency;
  private PaymentChannel channel;
  private PaymentStatus status;
  private String note;
  private String referenceNumber;
  private String failureReason;
  private Instant createdAt;
  private Instant updatedAt;
}
