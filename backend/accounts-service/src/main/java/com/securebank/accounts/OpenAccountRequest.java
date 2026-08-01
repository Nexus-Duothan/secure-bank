package com.securebank.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * @param productCode the bank product being opened; required, because an account
 *     can only be opened against a product the bank offers (FR-09).
 */
public record OpenAccountRequest(
  @NotBlank @Pattern(regexp = "SAVINGS|CURRENT") String accountType,
  @NotBlank String productCode,
  @NotBlank @Pattern(regexp = "INDIVIDUAL|JOINT") String ownershipType
) {}
