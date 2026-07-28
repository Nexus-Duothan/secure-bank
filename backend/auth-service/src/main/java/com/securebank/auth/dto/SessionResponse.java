package com.securebank.auth.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

  private UUID sessionId;
  private String ipAddress;
  private String userAgent;
  private String deviceInfo;
  private Instant createdAt;
  private Instant lastActiveAt;
  private Instant expiresAt;
  private boolean current;
}
