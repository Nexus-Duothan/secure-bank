package com.securebank.notification.dto;

import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

  private UUID id;
  private UUID userId;
  private NotificationType type;
  private NotificationChannel channel;
  private String title;
  private String message;
  private boolean read;
  private Instant createdAt;
  private String metadataJson;
}
