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

  @Value("${notification.sms.enabled:true}")
  private boolean enabled;

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
    boolean isSecurityFallback =
      type == NotificationType.SECURITY_ALERT && guaranteedSecurityFallback;
    if (!enabled && !isSecurityFallback) {
      log.debug("[SMS DISPATCHER] SMS dispatch disabled by configuration.");
      return false;
    }

    String phone = recipientContact != null ? recipientContact : "+94770000000";
    if (isSecurityFallback) {
      log.info(
        "[SMS DISPATCHER - GUARANTEED SECURITY FALLBACK] Sent priority SMS to {} for user {}: [{}] {}",
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
