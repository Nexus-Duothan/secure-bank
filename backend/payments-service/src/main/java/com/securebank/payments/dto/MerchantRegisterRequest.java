package com.securebank.payments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegisterRequest {

  @NotBlank(message = "Business name is required")
  private String businessName;

  private String category;

  @NotBlank(message = "Settlement account id is required")
  private String settlementAccountId;
}
