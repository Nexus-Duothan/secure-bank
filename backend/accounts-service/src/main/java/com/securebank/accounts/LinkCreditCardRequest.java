package com.securebank.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LinkCreditCardRequest(
  @NotBlank String cardNumber,
  @NotBlank @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}") String expiryDate,
  @NotBlank String nationalIdOrPassport,
  @NotBlank String accountId
) {}
