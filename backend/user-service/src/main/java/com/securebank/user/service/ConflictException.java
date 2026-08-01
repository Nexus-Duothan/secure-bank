package com.securebank.user.service;

/** Raised when a request conflicts with existing state, e.g. an email already taken. */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
