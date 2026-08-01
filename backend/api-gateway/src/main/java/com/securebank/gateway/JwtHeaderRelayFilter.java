package com.securebank.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.List;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtHeaderRelayFilter implements GlobalFilter, Ordered {

  private static final String USER_ID_HEADER = "X-User-Id";
  private static final String USER_ROLE_HEADER = "X-User-Role";
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  private final SecretKey key;

  @Value("${gateway.public-paths:/api/v1/auth/login,/api/v1/auth/register,/actuator/health}")
  private List<String> publicPaths;

  public JwtHeaderRelayFilter(@Value("${jwt.secret}") String secret) {
    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(secret);
    } catch (Exception e) {
      keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    // Strip any user-supplied X-User-Id / X-User-Role headers to prevent spoofing
    ServerHttpRequest.Builder requestBuilder = exchange
      .getRequest()
      .mutate()
      .headers(headers -> {
        headers.remove(USER_ID_HEADER);
        headers.remove(USER_ROLE_HEADER);
      });

    // Check if path is public
    if (isPublicPath(path)) {
      return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      return unauthorized(exchange, "Missing Authorization header");
    }

    String token = header.substring(7).trim();
    if (token.isEmpty()) {
      return unauthorized(exchange, "Missing Bearer token");
    }

    try {
      Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      String tokenType = claims.get("type", String.class);
      if (tokenType != null && !"ACCESS".equals(tokenType)) {
        return unauthorized(exchange, "Only access tokens can call downstream services");
      }

      String userId = claims.getSubject();
      String role = claims.get("role", String.class);
      requestBuilder.headers(headers -> {
        if (userId != null) {
          headers.set(USER_ID_HEADER, userId);
        }
        if (role != null && !role.isBlank()) {
          headers.set(USER_ROLE_HEADER, role);
        }
      });
      return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    } catch (JwtException | IllegalArgumentException exception) {
      return unauthorized(exchange, "Invalid or expired access token");
    }
  }

  private boolean isPublicPath(String path) {
    if (publicPaths == null) return false;
    return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] body = (
      "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" +
      message +
      "\"}"
    ).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return exchange
      .getResponse()
      .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
  }
}
