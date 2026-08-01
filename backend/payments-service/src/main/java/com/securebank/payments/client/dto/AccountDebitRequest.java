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
  private String reference;
}
