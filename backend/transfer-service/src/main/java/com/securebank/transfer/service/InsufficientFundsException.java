package com.securebank.transfer.service;

/** Raised when the source account doesn't have enough balance to cover a transfer plus fees. */
public class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException(String message) {
    super(message);
  }
}
