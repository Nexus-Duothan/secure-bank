package com.securebank.transfer.service;

/** Raised when a transfer would breach the per-transaction or daily aggregate limit. */
public class LimitExceededException extends RuntimeException {

  public LimitExceededException(String message) {
    super(message);
  }
}
