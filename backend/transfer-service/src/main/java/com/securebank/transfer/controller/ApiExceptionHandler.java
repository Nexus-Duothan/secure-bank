package com.securebank.transfer.controller;

import com.securebank.transfer.client.AccountNotFoundException;
import com.securebank.transfer.client.AccountsUnavailableException;
import com.securebank.transfer.security.AccessDeniedException;
import com.securebank.transfer.service.ConflictException;
import com.securebank.transfer.service.InsufficientFundsException;
import com.securebank.transfer.service.InvalidTransferStateException;
import com.securebank.transfer.service.LimitExceededException;
import com.securebank.transfer.service.OtpVerificationException;
import com.securebank.transfer.service.PayeeCoolingOffException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler({ EntityNotFoundException.class, AccountNotFoundException.class })
  public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException exception) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
    return error(HttpStatus.FORBIDDEN, exception.getMessage());
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<Map<String, Object>> handleInsufficientFunds(
    InsufficientFundsException exception
  ) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
  }

  @ExceptionHandler(LimitExceededException.class)
  public ResponseEntity<Map<String, Object>> handleLimitExceeded(LimitExceededException exception) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
  }

  @ExceptionHandler(InvalidTransferStateException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidState(
    InvalidTransferStateException exception
  ) {
    return error(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(PayeeCoolingOffException.class)
  public ResponseEntity<Map<String, Object>> handlePayeeCoolingOff(
    PayeeCoolingOffException exception
  ) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(ConflictException exception) {
    return error(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(OtpVerificationException.class)
  public ResponseEntity<Map<String, Object>> handleOtpFailure(OtpVerificationException exception) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
  }

  @ExceptionHandler(AccountsUnavailableException.class)
  public ResponseEntity<Map<String, Object>> handleAccountsUnavailable(
    AccountsUnavailableException exception
  ) {
    log.warn("accounts-service call failed", exception);
    return error(HttpStatus.SERVICE_UNAVAILABLE, "Unable to verify account balance, please try again");
  }

  /**
   * Backstop for a unique-constraint race that slips past an explicit check-then-insert (the
   * transfer idempotency key, the payee dedupe check, etc.) - deliberately generic since this
   * handler applies service-wide, not just to transfers.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(
    DataIntegrityViolationException exception
  ) {
    log.warn("Constraint violation while persisting a request", exception);
    return error(HttpStatus.CONFLICT, "That change conflicts with an existing record");
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    IllegalStateException.class,
    HttpMessageNotReadableException.class,
  })
  public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
    MethodArgumentNotValidException exception
  ) {
    String message = exception
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .findFirst()
      .map(error -> error.getField() + " " + error.getDefaultMessage())
      .orElse("Invalid request");
    return error(HttpStatus.BAD_REQUEST, message);
  }

  /** Never surface stack traces or internal messages to a banking client. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
    log.error("Unhandled failure in transfer-service", exception);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error, please try again");
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(
      Map.of(
        "timestamp",
        Instant.now(),
        "status",
        status.value(),
        "message",
        message == null ? status.getReasonPhrase() : message
      )
    );
  }
}
