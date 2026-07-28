package com.securebank.auth.security;

import com.securebank.auth.enums.Role;
import com.securebank.auth.enums.UserStatus;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long accessTokenExpirationMs;
  private final long refreshTokenExpirationMs;

  public JwtTokenProvider(
    @Value("${jwt.secret}") String secret,
    @Value("${jwt.access-token-expiration-seconds:300}") long accessTokenExpirationSeconds,
    @Value("${jwt.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds
  ) {
    byte[] keyBytes = Decoders.BASE64.decode(secret);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessTokenExpirationMs = accessTokenExpirationSeconds * 1000;
    this.refreshTokenExpirationMs = refreshTokenExpirationSeconds * 1000;
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

  public String generatePreAuthToken(UUID userId, String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + 300000); // 5 minutes

    return Jwts.builder()
      .subject(userId.toString())
      .claim("username", username)
      .claim("type", "PRE_AUTH")
      .issuedAt(now)
      .expiration(expiryDate)
      .signWith(key)
      .compact();
  }

  public String generateRefreshToken(UUID userId) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

    return Jwts.builder()
      .subject(userId.toString())
      .claim("type", "REFRESH")
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
    Claims claims = getClaimsFromToken(token);
    return UUID.fromString(claims.getSubject());
  }

  public String getUsernameFromToken(String token) {
    Claims claims = getClaimsFromToken(token);
    return claims.get("username", String.class);
  }

  public Role getRoleFromToken(String token) {
    Claims claims = getClaimsFromToken(token);
    String roleStr = claims.get("role", String.class);
    return roleStr != null ? Role.valueOf(roleStr) : null;
  }

  public UserStatus getStatusFromToken(String token) {
    Claims claims = getClaimsFromToken(token);
    String statusStr = claims.get("status", String.class);
    return statusStr != null ? UserStatus.valueOf(statusStr) : null;
  }

  public String getTokenTypeFromToken(String token) {
    Claims claims = getClaimsFromToken(token);
    return claims.get("type", String.class);
  }

  public long getAccessTokenExpirationSeconds() {
    return accessTokenExpirationMs / 1000;
  }
}
