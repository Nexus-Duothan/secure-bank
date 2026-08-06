package com.securebank.lending.enums;

public enum InstallmentStatus {
  PENDING,
  PAID,
  /** A collection attempt failed (insufficient funds / accounts-service unreachable); retry pending. */
  FAILED,
  /** Retry attempts exhausted without a successful collection. */
  OVERDUE,
}
