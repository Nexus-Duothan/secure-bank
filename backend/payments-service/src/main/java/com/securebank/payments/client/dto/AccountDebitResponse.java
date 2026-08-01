package com.securebank.payments.client.dto;

import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDebitResponse {

  private String accountId;
  private BigDecimal newBalance;
}
