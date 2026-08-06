package com.securebank.transfer.service;

/**
 * Raised when a transfer to a recently-added payee crosses the cooling-off threshold before that
 * payee's 12-hour safety window has elapsed.
 */
public class PayeeCoolingOffException extends RuntimeException {

  public PayeeCoolingOffException(String message) {
    super(message);
  }
}
