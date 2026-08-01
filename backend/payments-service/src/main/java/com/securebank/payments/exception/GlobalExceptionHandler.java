package com.securebank.payments.exception;

import com.securebank.payments.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The first @ControllerAdvice in this repo — auth-service/totp-service currently let
 * IllegalArgumentException/IllegalStateException fall through to Spring Boot's default
 * error body. This is scoped to payments-service only, not a retrofit of other services.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
    ResourceNotFoundException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<ErrorResponse> handleInsufficientFunds(
    InsufficientFundsException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request);
  }

  @ExceptionHandler(MerchantInactiveException.class)
  public ResponseEntity<ErrorResponse> handleMerchantInactive(
    MerchantInactiveException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
  }

  @ExceptionHandler(AccountsServiceUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleAccountsUnavailable(
    AccountsServiceUnavailableException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
  }

  @ExceptionHandler(InvalidQrPayloadException.class)
  public ResponseEntity<ErrorResponse> handleInvalidQr(
    InvalidQrPayloadException ex,
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

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
    IllegalArgumentException ex,
    HttpServletRequest request
  ) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
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
