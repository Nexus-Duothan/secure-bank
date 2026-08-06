package com.securebank.lending.enums;

public enum LoanStatus {
  ACTIVE,
  /** At least one installment has exhausted its repayment retry attempts. */
  DELINQUENT,
  PAID_OFF,
}
