package com.securebank.user.controller;

import com.securebank.user.security.AccessDeniedException;
import com.securebank.user.service.ConflictException;
import com.securebank.user.service.OtpVerificationException;
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

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException exception) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException exception) {
    return error(HttpStatus.FORBIDDEN, exception.getMessage());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(ConflictException exception) {
    return error(HttpStatus.CONFLICT, exception.getMessage());
  }

  /** Backstop for a unique-constraint race that slips past the explicit availability check. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(
    DataIntegrityViolationException exception
  ) {
    log.warn("Constraint violation while persisting user data", exception);
    return error(HttpStatus.CONFLICT, "That change conflicts with an existing record");
  }

  @ExceptionHandler(OtpVerificationException.class)
  public ResponseEntity<Map<String, Object>> handleOtpFailure(OtpVerificationException exception) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
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
    log.error("Unhandled failure in user-service", exception);
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
