package com.securebank.payments.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.securebank.payments.client.dto.AnomalyReport;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Stands in for security/audit-recovery-service in tests. recordEntry() is a no-op;
 * getAnomaliesForUser() returns a canned high-risk report for any userId tests have
 * flagged via flagUser(), letting PaymentControllerTest exercise the HELD_FOR_REVIEW path
 * without a live audit-recovery-service.
 */
@Component
@Primary
@Profile("test")
public class FakeAuditRecoveryClient extends AuditRecoveryClient {

  private static final Set<String> FLAGGED_USER_IDS = ConcurrentHashMap.newKeySet();

  public FakeAuditRecoveryClient() {
    super(WebClient.builder(), "http://127.0.0.1:8089", "test-key");
  }

  public static void flagUser(String userId) {
    FLAGGED_USER_IDS.add(userId);
  }

  public static void reset() {
    FLAGGED_USER_IDS.clear();
  }

  @Override
  public void recordEntry(String eventType, String userId, JsonNode payload) {
    // no-op — no live audit-recovery-service to call in tests
  }

  @Override
  public List<AnomalyReport> getAnomaliesForUser(String userId) {
    if (!FLAGGED_USER_IDS.contains(userId)) {
      return List.of();
    }
    return List.of(
      AnomalyReport.builder()
        .id("test-anomaly")
        .userId(userId)
        .eventCount(10)
        .riskScore(75)
        .reason("High-velocity transaction frequency detected (test)")
        .actionTaken("HIGH_RISK_FLAGGED")
        .status("ACTIVE")
        .detectionTimestamp(System.currentTimeMillis() / 1000)
        .build()
    );
  }
}
