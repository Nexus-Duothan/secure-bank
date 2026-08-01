package com.securebank.lending.enums;

/** FR-23 loan application lifecycle. */
public enum ApplicationStatus {
  SUBMITTED,
  UNDER_REVIEW,
  APPROVED,
  REJECTED,
  /** Terminal: the approved application's loan has been created and funds released. */
  DISBURSED,
}
