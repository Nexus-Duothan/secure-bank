package com.securebank.payments.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.securebank.payments.client.dto.AnomalyReport;
import com.securebank.payments.client.dto.AuditEntryRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Talks to the real, already-built security/audit-recovery-service (Rust/Axum, port 8089)
 * for FR-30/FR-31. That service's anomaly engine flags a user_id as high-velocity when
 * they generate >= 10 events of any type within a rolling 1-hour window (risk_score 75),
 * returned only while still "ACTIVE" — there is no server-side user_id filter on
 * GET /anomalies, so callers filter client-side. Not final so a test-profile subclass can
 * override both methods without a live audit-recovery-service.
 *
 * <p>Failures here are logged and swallowed rather than propagated: the audit/fraud hook
 * is a best-effort safety net, and a down audit-recovery-service should not block a
 * payment that already cleared the (separate, load-bearing) accounts-service debit call.
 */
@Component
public class AuditRecoveryClient {

  private static final Logger log = LoggerFactory.getLogger(AuditRecoveryClient.class);
  private static final String SERVICE_NAME = "payments-service";

  private final WebClient webClient;
  private final String apiKey;

  public AuditRecoveryClient(
    WebClient.Builder webClientBuilder,
    @Value("${audit-service.base-url}") String baseUrl,
    @Value("${audit-service.api-key}") String apiKey
  ) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    this.apiKey = apiKey;
  }

  public void recordEntry(String eventType, String userId, JsonNode payload) {
    AuditEntryRequest request = AuditEntryRequest.builder()
      .serviceName(SERVICE_NAME)
      .eventType(eventType)
      .userId(userId)
      .payload(payload)
      .build();

    try {
      webClient
        .post()
        .uri("/api/v1/audit/entries")
        .header("X-Internal-Service-Key", apiKey)
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .block();
    } catch (Exception ex) {
      log.warn("Failed to record audit entry for event {}: {}", eventType, ex.getMessage());
    }
  }

  public List<AnomalyReport> getAnomaliesForUser(String userId) {
    try {
      List<AnomalyReport> anomalies = webClient
        .get()
        .uri("/api/v1/audit/anomalies")
        .header("X-Internal-Service-Key", apiKey)
        .retrieve()
        .bodyToFlux(AnomalyReport.class)
        .collectList()
        .block();

      return anomalies == null
        ? List.of()
        : anomalies
            .stream()
            .filter(a -> userId.equals(a.getUserId()))
            .toList();
    } catch (Exception ex) {
      log.warn("Failed to fetch anomalies from audit-recovery-service: {}", ex.getMessage());
      return List.of();
    }
  }
}
