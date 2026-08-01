package com.securebank.notification.dispatcher;

import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationDispatcher implements NotificationDispatcher {

  @Value("${notification.email.enabled:true}")
  private boolean enabled;

  @Override
  public NotificationChannel getChannel() {
    return NotificationChannel.EMAIL;
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
      log.debug("[EMAIL DISPATCHER] Email dispatch disabled by configuration.");
      return false;
    }
    String recipient =
      recipientContact != null ? recipientContact : "user-" + userId + "@securebank.com";
    log.info(
      "[EMAIL DISPATCHER] Sent email to {} for user {}: [{}] {}",
      recipient,
      userId,
      title,
      message
    );
    return true;
  }
}
