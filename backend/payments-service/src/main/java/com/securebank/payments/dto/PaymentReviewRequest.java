package com.securebank.payments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReviewRequest {

  @NotNull(message = "Approve decision is required")
  private Boolean approve;

  private String note;
}
