package com.securebank.payments.exception;

public class AccountsServiceUnavailableException extends RuntimeException {

  public AccountsServiceUnavailableException(String message) {
    super(message);
  }

  public AccountsServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
