package com.securebank.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param nickname the customer's own name for the account; optional, the name the
 *     bank already holds for the account is kept when it is left empty.
 */
public record LinkAccountRequest(
  @NotBlank String accountNumber,
  @NotBlank String nationalIdOrPassport,
  @Size(max = 120) String nickname
) {}
