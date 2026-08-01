package com.securebank.lending.exception;

/** Raised when the linked account doesn't have enough balance to cover an installment. */
public class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException(String message) {
    super(message);
  }
}
