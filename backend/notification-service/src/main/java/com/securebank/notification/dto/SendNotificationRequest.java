package com.securebank.notification.dto;

import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequest {

  @NotNull(message = "userId is required")
  private UUID userId;

  @NotNull(message = "type is required")
  private NotificationType type;

  private NotificationChannel channel;

  @NotBlank(message = "title is required")
  private String title;

  @NotBlank(message = "message is required")
  private String message;

  private String recipientContact;

  private String metadataJson;
}
