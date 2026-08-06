package com.securebank.lending.enums;

/** The loan application lifecycle. */
public enum ApplicationStatus {
  SUBMITTED,
  UNDER_REVIEW,
  APPROVED,
  REJECTED,
  /** Terminal: the approved application's loan has been created and funds released. */
  DISBURSED,
}
