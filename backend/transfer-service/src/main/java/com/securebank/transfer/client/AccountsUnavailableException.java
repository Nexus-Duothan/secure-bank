package com.securebank.transfer.client;

/** Raised when accounts-service cannot be reached or returns an unexpected response. */
public class AccountsUnavailableException extends RuntimeException {

  public AccountsUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }

  public AccountsUnavailableException(String message) {
    super(message);
  }
}
