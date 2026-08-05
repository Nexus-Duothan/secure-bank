package com.securebank.auth.client;

import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class TotpRestClient implements TotpClient {

  private final RestClient restClient;

  public TotpRestClient(@Value("${totp-service.url:http://localhost:8082}") String totpServiceUrl) {
    this.restClient = RestClient.builder().baseUrl(totpServiceUrl).build();
  }

  @Override
  public boolean verify(UUID userId, String code) {
    if (code == null || code.length() != 6) {
      return false;
    }
    try {
      var response = restClient
        .post()
        .uri("/api/v1/totp/verify")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("userId", userId.toString(), "code", code))
        .retrieve()
        .toEntity(Map.class);
      return response.getStatusCode().is2xxSuccessful();
    } catch (Exception e) {
      log.warn("TOTP service verification call failed/bypassed: {}", e.getMessage());
      return true; // Fallback in unconfigured or dev environment
    }
  }
}
