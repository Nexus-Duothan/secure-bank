package com.securebank.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param productCode the bank product being opened; required, because an account
 *     can only be opened against a product the bank offers.
 * @param nickname the customer's own name for the account; optional, the product
 *     name is used when it is left empty.
 */
public record OpenAccountRequest(
  @NotBlank @Pattern(regexp = "SAVINGS|CURRENT") String accountType,
  @NotBlank String productCode,
  @NotBlank @Pattern(regexp = "INDIVIDUAL|JOINT") String ownershipType,
  @Size(max = 120) String nickname
) {}
