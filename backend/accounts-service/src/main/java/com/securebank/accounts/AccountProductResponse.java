package com.securebank.accounts;

import java.math.BigDecimal;

/**
 * An account product the bank offers (FR-09). Customers may only open an account
 * against one of these; the product decides the interest rate, the minimum
 * balance, and the monthly fee, so it is never free text.
 */
public record AccountProductResponse(
  String code,
  String name,
  String accountType,
  String description,
  String currency,
  double interestRate,
  BigDecimal minimumBalance,
  BigDecimal monthlyFee
) {}
