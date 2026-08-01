package com.securebank.notification.dispatcher;

import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.util.UUID;

public interface NotificationDispatcher {
  NotificationChannel getChannel();

  boolean dispatch(
    UUID userId,
    NotificationType type,
    String title,
    String message,
    String recipientContact
  );
}
