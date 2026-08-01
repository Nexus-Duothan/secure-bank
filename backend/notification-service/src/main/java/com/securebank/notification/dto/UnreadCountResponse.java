package com.securebank.notification.dto;

import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadCountResponse {

  private UUID userId;
  private long unreadCount;
}
