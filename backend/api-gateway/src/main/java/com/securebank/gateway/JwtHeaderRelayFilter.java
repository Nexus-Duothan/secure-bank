package com.securebank.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.List;
import java.util.Map;
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
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final SecretKey key;

  @Value("${gateway.public-paths:/api/v1/auth/**,/actuator/health}")
  private List<String> publicPaths;

  public JwtHeaderRelayFilter(@Value("${jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
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
      if (!"ACCESS".equals(tokenType)) {
        return unauthorized(exchange, "Only access tokens can call downstream services");
      }

      String userId = claims.getSubject();
      String role = claims.get("role", String.class);

      if (userId == null || userId.isEmpty()) {
        return unauthorized(exchange, "Token payload missing subject");
      }

      requestBuilder.header(USER_ID_HEADER, userId);
      if (role != null) {
        requestBuilder.header(USER_ROLE_HEADER, role);
      }

      return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    } catch (JwtException | IllegalArgumentException exception) {
      return unauthorized(exchange, "Invalid or expired access token");
    }
  }

  private boolean isPublicPath(String path) {
    if (path != null && path.startsWith("/api/v1/auth/")) {
      return true;
    }
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

    Map<String, Object> errorPayload = Map.of(
      "status",
      401,
      "error",
      "Unauthorized",
      "message",
      message,
      "path",
      exchange.getRequest().getURI().getPath()
    );

    byte[] body;
    try {
      body = objectMapper.writeValueAsBytes(errorPayload);
    } catch (Exception e) {
      body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Unauthorized\"}".getBytes(
        java.nio.charset.StandardCharsets.UTF_8
      );
    }

    return exchange
      .getResponse()
      .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
  }
}
