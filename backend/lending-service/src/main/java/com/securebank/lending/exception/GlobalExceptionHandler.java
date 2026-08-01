package com.securebank.lending.exception;

import com.securebank.lending.client.AccountNotFoundException;
import com.securebank.lending.client.AccountsUnavailableException;
import com.securebank.lending.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler({ ResourceNotFoundException.class, AccountNotFoundException.class })
  public ResponseEntity<ErrorResponse> handleNotFound(
    RuntimeException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientFunds(
    InsufficientFundsException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
  }

  @ExceptionHandler(LoanLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleLimitExceeded(
    LoanLimitExceededException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
  }

  @ExceptionHandler(InvalidApplicationStateException.class)
  public ResponseEntity<ErrorResponse> handleInvalidState(
    InvalidApplicationStateException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(AccountsUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleAccountsUnavailable(
    AccountsUnavailableException ex,
    HttpServletRequest request
  ) {
    log.warn("accounts-service call failed", ex);
    return build(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Unable to verify account balance, please try again",
      request
    );
  }

  /**
   * Without this, Spring Security's AccessDeniedException (thrown by a failed
   * {@code @PreAuthorize} check on an officer-only endpoint) would fall through to the
   * generic Exception.class handler below and surface as a 500 instead of a 403.
   */
  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
    org.springframework.security.access.AccessDeniedException ex,
    HttpServletRequest request
  ) {
    return build(
      HttpStatus.FORBIDDEN,
      "You do not have permission to perform this action",
      request
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
    MethodArgumentNotValidException ex,
    HttpServletRequest request
  ) {
    String message = ex
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .findFirst()
      .map(error -> error.getField() + ": " + error.getDefaultMessage())
      .orElse("Validation failed");
    return build(HttpStatus.BAD_REQUEST, message, request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
    IllegalArgumentException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(
    IllegalStateException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  /** Never surface stack traces or internal messages to a banking client. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unhandled failure in lending-service", ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again", request);
  }

  private ResponseEntity<ErrorResponse> build(
    HttpStatus status,
    String message,
    HttpServletRequest request
  ) {
    ErrorResponse body = ErrorResponse.builder()
      .timestamp(Instant.now())
      .status(status.value())
      .error(status.getReasonPhrase())
      .message(message)
      .path(request.getRequestURI())
      .build();
    return ResponseEntity.status(status).body(body);
  }
}
