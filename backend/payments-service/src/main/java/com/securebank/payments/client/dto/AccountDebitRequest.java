package com.securebank.payments.client.dto;

import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDebitRequest {

  private BigDecimal amount;
  private String currency;

  /** Idempotency key, so a retried debit after a timeout cannot charge the customer twice. */
  private String reference;

  // How the movement should read on the customer's statement.
  private String merchant;
  private String category;
  private String transactionType;
  private String location;
}
