package com.securebank.payments.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.*;

/** accounts-service also returns the ledger ids it created; payments only needs the balance. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDebitResponse {

  private String accountId;
  private BigDecimal newBalance;
}
