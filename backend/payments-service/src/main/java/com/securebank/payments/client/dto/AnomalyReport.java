package com.securebank.payments.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/** Mirrors security/audit-recovery-service's Rust AnomalyReport (snake_case wire format). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyReport {

  private String id;

  @JsonProperty("user_id")
  private String userId;

  @JsonProperty("event_count")
  private int eventCount;

  @JsonProperty("risk_score")
  private int riskScore;

  private String reason;

  @JsonProperty("action_taken")
  private String actionTaken;

  private String status;

  @JsonProperty("detection_timestamp")
  private long detectionTimestamp;
}
