package com.securebank.accounts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Reads the caller's real profile from user-service so an opened account is stamped with the
 * customer's own name and address (used on cards and the statement PDF). Best effort: if
 * user-service cannot be reached the account is still created, just without the holder details.
 */
@Component
@Slf4j
public class UserProfileClient {

  private final RestClient restClient;

  public UserProfileClient(
    @Value("${user-service.url:http://localhost:8083}") String userServiceUrl
  ) {
    this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
  }

  public Optional<UserProfileSnapshot> getProfile(String authorizationHeader, String callerUserId) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(
        restClient
          .get()
          .uri("/api/v1/users/me")
          .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
          .retrieve()
          .body(UserProfileSnapshot.class)
      );
    } catch (Exception exception) {
      log.warn(
        "Could not read the profile of {} from user-service: {}",
        callerUserId,
        exception.getMessage()
      );
      return Optional.empty();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record UserProfileSnapshot(String fullName, String addressLine, String city) {}
}
