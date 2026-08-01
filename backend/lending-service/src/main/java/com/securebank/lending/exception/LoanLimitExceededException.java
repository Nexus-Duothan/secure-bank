package com.securebank.lending.exception;

/** Raised when a loan application requests an amount or term outside the platform's configured bounds (FR-22). */
public class LoanLimitExceededException extends RuntimeException {

  public LoanLimitExceededException(String message) {
    super(message);
  }
}
