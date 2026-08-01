package com.securebank.notification.dispatcher;

import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PushNotificationDispatcher implements NotificationDispatcher {

  @Value("${notification.push.enabled:true}")
  private boolean enabled;

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
    if (!enabled) {
      log.debug("[PUSH DISPATCHER] Push notification dispatch disabled by configuration.");
      return false;
    }
    log.info(
      "[PUSH DISPATCHER] Sent mobile push notification to user {}: [{}] {}",
      userId,
      title,
      message
    );
    return true;
  }
}
