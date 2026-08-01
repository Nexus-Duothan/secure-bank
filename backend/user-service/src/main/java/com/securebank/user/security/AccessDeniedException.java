package com.securebank.user.security;

/** Raised when a caller is unauthenticated or lacks the role required for an operation. */
public class AccessDeniedException extends RuntimeException {

  public AccessDeniedException(String message) {
    super(message);
  }
}
