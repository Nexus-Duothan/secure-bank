package com.securebank.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayRequest {

  @NotBlank(message = "Merchant code is required")
  private String merchantCode;

  @NotNull(message = "Amount is required")
  @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
  private BigDecimal amount;

  @NotBlank(message = "Currency is required")
  private String currency;

  private String note;
}
