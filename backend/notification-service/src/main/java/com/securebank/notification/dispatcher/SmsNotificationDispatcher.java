package com.securebank.notification.dispatcher;

import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsNotificationDispatcher implements NotificationDispatcher {

  @Value("${notification.sms.guaranteed-security-fallback:true}")
  private boolean guaranteedSecurityFallback;

  @Override
  public NotificationChannel getChannel() {
    return NotificationChannel.SMS;
  }

  @Override
  public boolean dispatch(
    UUID userId,
    NotificationType type,
    String title,
    String message,
    String recipientContact
  ) {
    String phone = recipientContact != null ? recipientContact : "+94770000000";
    if (type == NotificationType.SECURITY_ALERT && guaranteedSecurityFallback) {
      log.info(
        "[SMS DISPATCHER - GUARANTEED SECURITY FALLBACK (FR-29)] Sent priority SMS to {} for user {}: [{}] {}",
        phone,
        userId,
        title,
        message
      );
    } else {
      log.info(
        "[SMS DISPATCHER] Sent SMS to {} for user {}: [{}] {}",
        phone,
        userId,
        title,
        message
      );
    }
    return true;
  }
}
