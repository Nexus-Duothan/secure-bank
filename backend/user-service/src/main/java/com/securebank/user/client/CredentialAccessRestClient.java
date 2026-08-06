package com.securebank.user.client;

import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class CredentialAccessRestClient implements CredentialAccessClient {

  private final RestClient restClient;

  public CredentialAccessRestClient(
    @Value("${auth-service.url:http://localhost:8081}") String authServiceUrl
  ) {
    this.restClient = RestClient.builder().baseUrl(authServiceUrl).build();
  }

  @Override
  public void updateAccess(UUID userId, UserStatus status, Role role) {
    Map<String, Object> body = new HashMap<>();
    if (status != null) {
      body.put("status", toCredentialStatus(status));
    }
    if (role != null) {
      // Both services name roles the same way, so this crosses over unchanged.
      body.put("role", role.name());
    }
    if (body.isEmpty()) {
      return;
    }

    restClient
      .put()
      .uri("/internal/v1/credentials/{userId}/access", userId)
      .contentType(MediaType.APPLICATION_JSON)
      .body(body)
      .retrieve()
      .toBodilessEntity();
  }

  /**
   * The two services do not name statuses identically: this service tracks the servicing view of
   * an account, auth-service tracks whether the credentials may sign in. A suspended account is a
   * blocked sign-in, and one waiting on review has not been let in yet.
   */
  private String toCredentialStatus(UserStatus status) {
    return switch (status) {
      case ACTIVE -> "ACTIVE";
      case FROZEN -> "FROZEN";
      case SUSPENDED -> "BLOCKED";
      case PENDING_REVIEW -> "UNDER_REVIEW";
    };
  }
}
