package com.securebank.transfer.security;

/** Raised when a caller is unauthenticated or attempts to act on another user's resources. */
public class AccessDeniedException extends RuntimeException {

  public AccessDeniedException(String message) {
    super(message);
  }
}
