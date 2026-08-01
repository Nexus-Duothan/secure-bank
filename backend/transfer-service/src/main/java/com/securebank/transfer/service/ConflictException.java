package com.securebank.transfer.service;

/** Raised when a request conflicts with existing state, e.g. a payee that's already saved. */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
