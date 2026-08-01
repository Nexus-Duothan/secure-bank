package com.securebank.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = "An unexpected error occurred at Gateway";

    if (ex instanceof ResponseStatusException rse) {
      status = HttpStatus.valueOf(rse.getStatusCode().value());
      message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
    }

    log.error(
      "Gateway Exception on {}: {}",
      exchange.getRequest().getURI().getPath(),
      ex.getMessage()
    );

    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> errorBody = Map.of(
      "status",
      status.value(),
      "error",
      status.getReasonPhrase(),
      "message",
      message,
      "path",
      exchange.getRequest().getURI().getPath()
    );

    byte[] bytes;
    try {
      bytes = objectMapper.writeValueAsBytes(errorBody);
    } catch (Exception serializationException) {
      bytes =
        "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"An unexpected error occurred at Gateway\"}".getBytes(
          java.nio.charset.StandardCharsets.UTF_8
        );
    }

    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }
}
