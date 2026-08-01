package com.securebank.payments.exception;

public class MerchantInactiveException extends RuntimeException {

  public MerchantInactiveException(String message) {
    super(message);
  }
}
