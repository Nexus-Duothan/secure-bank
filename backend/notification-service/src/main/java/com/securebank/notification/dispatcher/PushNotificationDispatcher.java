package com.securebank.notification.dispatcher;

import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushNotificationDispatcher implements NotificationDispatcher {

  @Override
  public NotificationChannel getChannel() {
    return NotificationChannel.PUSH;
  }

  @Override
  public boolean dispatch(
    UUID userId,
    NotificationType type,
    String title,
    String message,
    String recipientContact
  ) {
    log.info(
      "[PUSH DISPATCHER] Sent mobile push notification to user {}: [{}] {}",
      userId,
      title,
      message
    );
    return true;
  }
}
