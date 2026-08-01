package com.securebank.accounts;

import jakarta.validation.constraints.NotBlank;

public record LinkAccountRequest(
  @NotBlank String accountNumber,
  @NotBlank String nationalIdOrPassport
) {}
