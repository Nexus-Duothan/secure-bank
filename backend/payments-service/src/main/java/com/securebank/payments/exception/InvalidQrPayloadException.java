package com.securebank.payments.exception;

public class InvalidQrPayloadException extends RuntimeException {

  public InvalidQrPayloadException(String message) {
    super(message);
  }
}
