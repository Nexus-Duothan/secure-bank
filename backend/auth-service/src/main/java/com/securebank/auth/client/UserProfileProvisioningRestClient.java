package com.securebank.auth.client;

import com.securebank.auth.entity.UserCredential;
import com.securebank.auth.enums.UserStatus;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class UserProfileProvisioningRestClient implements UserProfileProvisioningClient {

  private final RestClient restClient;

  public UserProfileProvisioningRestClient(
    @Value("${user-service.url:http://localhost:8083}") String userServiceUrl
  ) {
    this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
  }

  @Override
  public boolean provision(UserCredential user) {
    try {
      restClient
        .post()
        .uri("/internal/v1/users")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          Map.of(
            "id",
            user.getId().toString(),
            "fullName",
            user.getFullName(),
            "email",
            user.getEmail(),
            "phoneNumber",
            user.getPhoneNumber() == null ? "" : user.getPhoneNumber(),
            "role",
            user.getRole().name(),
            "status",
            profileStatus(user.getStatus())
          )
        )
        .retrieve()
        .toBodilessEntity();
      return true;
    } catch (Exception exception) {
      // Registration itself has already succeeded; the next successful sign-in retries this.
      log.warn(
        "Could not provision the profile for {} in user-service: {}",
        user.getId(),
        exception.getMessage()
      );
      return false;
    }
  }

  /** user-service tracks a smaller set of states than the credential store does. */
  private String profileStatus(UserStatus status) {
    return switch (status) {
      case ACTIVE -> "ACTIVE";
      case FROZEN -> "FROZEN";
      case BLOCKED, REJECTED -> "SUSPENDED";
      case PENDING_KYC, UNDER_REVIEW -> "PENDING_REVIEW";
    };
  }
}
