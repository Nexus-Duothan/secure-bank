package com.securebank.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtHeaderRelayFilter implements GlobalFilter, Ordered {

  private static final String USER_ID_HEADER = "X-User-Id";
  private static final String USER_ROLE_HEADER = "X-User-Role";

  private final SecretKey key;

  public JwtHeaderRelayFilter(@Value("${jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    ServerHttpRequest.Builder requestBuilder = exchange
      .getRequest()
      .mutate()
      .headers(headers -> {
        headers.remove(USER_ID_HEADER);
        headers.remove(USER_ROLE_HEADER);
      });

    if (header == null || !header.startsWith("Bearer ")) {
      return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    String token = header.substring(7).trim();
    if (token.isEmpty()) {
      return unauthorized(exchange, "Missing Bearer token");
    }

    try {
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      String tokenType = claims.get("type", String.class);
      if (!"ACCESS".equals(tokenType)) {
        return unauthorized(exchange, "Only access tokens can call downstream services");
      }

      String userId = claims.getSubject();
      String role = claims.get("role", String.class);
      requestBuilder.headers(headers -> {
        headers.set(USER_ID_HEADER, userId);
        if (role != null && !role.isBlank()) {
          headers.set(USER_ROLE_HEADER, role);
        }
      });
      return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    } catch (JwtException | IllegalArgumentException exception) {
      return unauthorized(exchange, "Invalid or expired access token");
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] body = ("{\"message\":\"" + message + "\"}").getBytes(
      java.nio.charset.StandardCharsets.UTF_8
    );
    return exchange
      .getResponse()
      .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
  }
}
