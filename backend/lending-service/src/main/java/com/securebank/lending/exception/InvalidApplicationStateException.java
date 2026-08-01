package com.securebank.lending.exception;

/** Raised when an operation is attempted against a loan application that isn't in a valid state for it. */
public class InvalidApplicationStateException extends RuntimeException {

  public InvalidApplicationStateException(String message) {
    super(message);
  }
}
