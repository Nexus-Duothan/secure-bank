package com.securebank.payments.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

/** Mirrors security/audit-recovery-service's Rust CreateAuditEntryRequest (snake_case wire format). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntryRequest {

  @JsonProperty("service_name")
  private String serviceName;

  @JsonProperty("event_type")
  private String eventType;

  @JsonProperty("user_id")
  private String userId;

  private JsonNode payload;
}
