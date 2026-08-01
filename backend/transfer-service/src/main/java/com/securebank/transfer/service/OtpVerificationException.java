package com.securebank.transfer.service;

/**
 * Raised when a submitted one-time code is wrong, expired, or has exhausted its attempt budget.
 *
 * <p>Declared as {@code noRollbackFor} on the confirmation transaction so the failed-attempt
 * counter it increments survives; otherwise the rollback would reset the counter and the six digit
 * code could be brute forced without limit.
 */
public class OtpVerificationException extends RuntimeException {

  public OtpVerificationException(String message) {
    super(message);
  }
}
