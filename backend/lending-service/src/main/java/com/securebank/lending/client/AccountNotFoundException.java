package com.securebank.lending.client;

/** Raised when accounts-service has no account matching the requested id. */
public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(String message) {
    super(message);
  }
}
