package com.securebank.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayIntegrationTest {

  @Autowired
  private WebTestClient webTestClient;

  private String secret =
    "dGhpc0lzQVZlcnlTZWN1cmVTZWNyZXRLZXlGb3JTZWN1cmVCYW5rSkdUVG9rZW5zMjAyNiE=";
  private String validAccessToken;
  private String tokenWithoutType;
  private String refreshToken;
  private String invalidToken;

  @BeforeEach
  void setUp() {
    byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secret);
    validAccessToken = Jwts.builder()
      .subject(UUID.randomUUID().toString())
      .claim("role", "CUSTOMER")
      .claim("type", "ACCESS")
      .expiration(new Date(System.currentTimeMillis() + 3600000))
      .signWith(Keys.hmacShaKeyFor(keyBytes))
      .compact();

    tokenWithoutType = Jwts.builder()
      .subject(UUID.randomUUID().toString())
      .claim("role", "CUSTOMER")
      .expiration(new Date(System.currentTimeMillis() + 3600000))
      .signWith(Keys.hmacShaKeyFor(keyBytes))
      .compact();

    refreshToken = Jwts.builder()
      .subject(UUID.randomUUID().toString())
      .claim("role", "CUSTOMER")
      .claim("type", "REFRESH")
      .expiration(new Date(System.currentTimeMillis() + 3600000))
      .signWith(Keys.hmacShaKeyFor(keyBytes))
      .compact();

    invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalidTokenPayload.signature";
  }

  @Test
  void publicEndpoint_BypassesAuthFilter() {
    webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
  }

  @Test
  void registrationPhoneVerificationEndpoint_BypassesAuthFilter() {
    webTestClient
      .post()
      .uri("/api/v1/auth/register/verify-phone")
      .exchange()
      .expectStatus()
      .value(status -> org.junit.jupiter.api.Assertions.assertNotEquals(401, status));
  }

  @Test
  void totpSetupEndpoint_BypassesAuthFilterForNewlyRegisteredUsers() {
    webTestClient
      .post()
      .uri("/api/v1/totp/setup/{userId}", UUID.randomUUID())
      .exchange()
      .expectStatus()
      .value(status -> org.junit.jupiter.api.Assertions.assertNotEquals(401, status));
  }

  @Test
  void totpEnableEndpoint_BypassesAuthFilterForNewlyRegisteredUsers() {
    webTestClient
      .post()
      .uri("/api/v1/totp/enable")
      .exchange()
      .expectStatus()
      .value(status -> org.junit.jupiter.api.Assertions.assertNotEquals(401, status));
  }

  @Test
  void protectedEndpoint_WithoutToken_Returns401Unauthorized() {
    webTestClient
      .get()
      .uri("/api/v1/payments/123")
      .exchange()
      .expectStatus()
      .isUnauthorized()
      .expectBody()
      .jsonPath("$.status")
      .isEqualTo(401)
      .jsonPath("$.error")
      .isEqualTo("Unauthorized");
  }

  @Test
  void protectedEndpoint_WithInvalidToken_Returns401Unauthorized() {
    webTestClient
      .get()
      .uri("/api/v1/payments/123")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken)
      .exchange()
      .expectStatus()
      .isUnauthorized();
  }

  @Test
  void protectedEndpoint_WithValidAccessToken_PassesAuthFilter() {
    // Valid token passes filter and attempts downstream routing (returning 503 or 500 error instead of 401)
    webTestClient
      .get()
      .uri("/api/v1/payments/123")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAccessToken)
      .exchange()
      .expectStatus()
      .value(status -> org.junit.jupiter.api.Assertions.assertNotEquals(401, status));
  }

  @Test
  void protectedEndpoint_WithTokenMissingTypeClaim_Returns401Unauthorized() {
    webTestClient
      .get()
      .uri("/api/v1/payments/123")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutType)
      .exchange()
      .expectStatus()
      .isUnauthorized();
  }

  @Test
  void protectedEndpoint_WithRefreshToken_Returns401Unauthorized() {
    webTestClient
      .get()
      .uri("/api/v1/payments/123")
      .header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken)
      .exchange()
      .expectStatus()
      .isUnauthorized();
  }

  @Test
  void corsPreflight_ReturnsPermissiveCorsHeaders() {
    webTestClient
      .options()
      .uri("/api/v1/auth/login")
      .header(HttpHeaders.ORIGIN, "http://localhost:5173")
      .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
  }
}
