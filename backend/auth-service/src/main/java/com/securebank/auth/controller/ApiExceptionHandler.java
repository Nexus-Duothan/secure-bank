package com.securebank.auth.controller;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
    MethodArgumentNotValidException exception
  ) {
    FieldError fieldError = exception
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .findFirst()
      .orElse(null);
    String message =
      fieldError != null ? fieldError.getDefaultMessage() : "Request validation failed";
    return build(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraintViolation(
    ConstraintViolationException exception
  ) {
    String message = exception
      .getConstraintViolations()
      .stream()
      .findFirst()
      .map(violation -> violation.getMessage())
      .orElse("Request validation failed");
    return build(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
    return build(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException exception) {
    return build(HttpStatus.CONFLICT, exception.getMessage());
  }

  private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(
      Map.of(
        "timestamp",
        Instant.now().toString(),
        "status",
        status.value(),
        "error",
        status.getReasonPhrase(),
        "message",
        message
      )
    );
  }
}
