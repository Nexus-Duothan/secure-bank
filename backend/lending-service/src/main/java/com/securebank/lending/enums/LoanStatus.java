package com.securebank.lending.enums;

public enum LoanStatus {
  ACTIVE,
  /** At least one installment has exhausted its repayment retry attempts (FR-25). */
  DELINQUENT,
  PAID_OFF,
}
