package com.securebank.transfer.service;

/** Raised when an operation is attempted against a transfer that isn't in a valid state for it. */
public class InvalidTransferStateException extends RuntimeException {

  public InvalidTransferStateException(String message) {
    super(message);
  }
}
