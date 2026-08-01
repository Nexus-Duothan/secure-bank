package com.securebank.accounts;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatus(
    ResponseStatusException exception
  ) {
    String message =
      exception.getReason() == null
        ? "The account request could not be completed"
        : exception.getReason();

    return ResponseEntity.status(exception.getStatusCode()).body(
      Map.of(
        "timestamp",
        Instant.now().toString(),
        "status",
        exception.getStatusCode().value(),
        "message",
        message
      )
    );
  }
}
