package com.securebank.notification.exception;

import com.securebank.notification.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
    ResourceNotFoundException ex,
    HttpServletRequest request
  ) {
    ErrorResponse error = ErrorResponse.builder()
      .status(HttpStatus.NOT_FOUND.value())
      .error("Not Found")
      .message(ex.getMessage())
      .path(request.getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
    MethodArgumentNotValidException ex,
    HttpServletRequest request
  ) {
    String msg = ex
      .getBindingResult()
      .getFieldErrors()
      .stream()
      .map(e -> e.getField() + ": " + e.getDefaultMessage())
      .findFirst()
      .orElse("Validation failed");

    ErrorResponse error = ErrorResponse.builder()
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Bad Request")
      .message(msg)
      .path(request.getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    ErrorResponse error = ErrorResponse.builder()
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error("Internal Server Error")
      .message(ex.getMessage())
      .path(request.getRequestURI())
      .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
