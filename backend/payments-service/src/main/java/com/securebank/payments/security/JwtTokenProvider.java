package com.securebank.payments.security;

import com.securebank.payments.enums.Role;
import com.securebank.payments.enums.UserStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates access tokens minted by auth-service. payments-service shares auth-service's
 * jwt.secret so tokens issued there validate here without a shared auth module. This
 * service never issues pre-auth/refresh tokens — generateAccessToken exists only so tests
 * can mint a valid token without running auth-service.
 */
@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long accessTokenExpirationMs;

  public JwtTokenProvider(
    @Value("${jwt.secret}") String secret,
    @Value("${jwt.access-token-expiration-seconds:300}") long accessTokenExpirationSeconds
  ) {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessTokenExpirationMs = accessTokenExpirationSeconds * 1000;
  }

  public String generateAccessToken(UUID userId, String username, Role role, UserStatus status) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

    return Jwts.builder()
      .subject(userId.toString())
      .claim("username", username)
      .claim("role", role.name())
      .claim("status", status.name())
      .claim("type", "ACCESS")
      .issuedAt(now)
      .expiration(expiryDate)
      .signWith(key)
      .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public Claims getClaimsFromToken(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public UUID getUserIdFromToken(String token) {
    return UUID.fromString(getClaimsFromToken(token).getSubject());
  }

  public Role getRoleFromToken(String token) {
    String roleStr = getClaimsFromToken(token).get("role", String.class);
    return roleStr != null ? Role.valueOf(roleStr) : null;
  }

  public String getTokenTypeFromToken(String token) {
    return getClaimsFromToken(token).get("type", String.class);
  }
}
