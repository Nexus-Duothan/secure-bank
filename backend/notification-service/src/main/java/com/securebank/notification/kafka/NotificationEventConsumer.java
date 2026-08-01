package com.securebank.notification.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import com.securebank.notification.service.MultiChannelNotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

  private final MultiChannelNotificationService notificationService;
  private final ObjectMapper objectMapper;

  @KafkaListener(
    topics = { "auth.events.v1", "auth.events" },
    groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
  )
  public void handleAuthEvents(String message) {
    try {
      JsonNode json = objectMapper.readTree(message);
      String eventType = json.path("eventType").asText(json.path("event_type").asText(""));
      String userIdStr = json.path("userId").asText(json.path("user_id").asText(""));

      if (userIdStr.isEmpty()) return;
      UUID userId = UUID.fromString(userIdStr);

      if ("FAILED_LOGIN".equalsIgnoreCase(eventType)) {
        notificationService.sendNotification(
          userId,
          NotificationType.SECURITY_ALERT,
          NotificationChannel.SMS,
          "Security Alert: Failed Login Attempt",
          "A failed login attempt was detected on your SecureBank account.",
          null,
          message
        );
      } else if ("LOGIN_SUCCESS".equalsIgnoreCase(eventType)) {
        notificationService.sendNotification(
          userId,
          NotificationType.ACCOUNT_ALERT,
          NotificationChannel.IN_APP,
          "Successful Login",
          "You signed in to your SecureBank account.",
          null,
          message
        );
      }
    } catch (Exception e) {
      log.warn("Failed to process auth event for notification: {}", e.getMessage());
    }
  }

  @KafkaListener(
    topics = "user.events.v1",
    groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
  )
  public void handleUserEvents(String message) {
    try {
      JsonNode json = objectMapper.readTree(message);
      String eventType = json.path("eventType").asText(json.path("event_type").asText(""));
      String userIdStr = json.path("userId").asText(json.path("user_id").asText(""));

      if (userIdStr.isEmpty()) return;
      UUID userId = UUID.fromString(userIdStr);

      if ("PROFILE_UPDATED".equalsIgnoreCase(eventType)) {
        notificationService.sendNotification(
          userId,
          NotificationType.ACCOUNT_ALERT,
          NotificationChannel.IN_APP,
          "Profile Updated",
          "Your user profile information was successfully updated.",
          null,
          message
        );
      } else if ("DEVICE_LINKED".equalsIgnoreCase(eventType)) {
        notificationService.sendNotification(
          userId,
          NotificationType.SECURITY_ALERT,
          NotificationChannel.SMS,
          "Security Alert: New Device Linked",
          "A new trusted device was registered to your SecureBank account.",
          null,
          message
        );
      } else if ("MFA_ENABLED".equalsIgnoreCase(eventType)) {
        notificationService.sendNotification(
          userId,
          NotificationType.SECURITY_ALERT,
          NotificationChannel.SMS,
          "Security Alert: MFA Enabled",
          "Multi-Factor Authentication (TOTP) has been enabled for your account.",
          null,
          message
        );
      }
    } catch (Exception e) {
      log.warn("Failed to process user event for notification: {}", e.getMessage());
    }
  }

  @KafkaListener(
    topics = "transfer.completed.v1",
    groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
  )
  public void handleTransferCompleted(String message) {
    try {
      JsonNode json = objectMapper.readTree(message);
      String userIdStr = json.path("payerUserId").asText(json.path("user_id").asText(""));
      String amount = json.path("amount").asText("0");
      String currency = json.path("currency").asText("LKR");

      if (userIdStr.isEmpty()) return;
      UUID userId = UUID.fromString(userIdStr);

      notificationService.sendNotification(
        userId,
        NotificationType.TRANSACTION_ALERT,
        NotificationChannel.EMAIL,
        "Fund Transfer Completed",
        String.format("Transfer of %s %s was successfully completed.", currency, amount),
        null,
        message
      );
    } catch (Exception e) {
      log.warn("Failed to process transfer event for notification: {}", e.getMessage());
    }
  }

  @KafkaListener(
    topics = "payments.completed.v1",
    groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
  )
  public void handlePaymentCompleted(String message) {
    try {
      JsonNode json = objectMapper.readTree(message);
      String userIdStr = json.path("payerUserId").asText(json.path("user_id").asText(""));
      String amount = json.path("amount").asText("0");

      if (userIdStr.isEmpty()) return;
      UUID userId = UUID.fromString(userIdStr);

      notificationService.sendNotification(
        userId,
        NotificationType.TRANSACTION_ALERT,
        NotificationChannel.IN_APP,
        "Vendor Payment Completed",
        String.format("Merchant payment of LKR %s completed successfully.", amount),
        null,
        message
      );
    } catch (Exception e) {
      log.warn("Failed to process payment completed event for notification: {}", e.getMessage());
    }
  }

  @KafkaListener(
    topics = "payments.held-for-review.v1",
    groupId = "${spring.kafka.consumer.group-id:notification-service-group}"
  )
  public void handlePaymentHeld(String message) {
    try {
      JsonNode json = objectMapper.readTree(message);
      String userIdStr = json.path("payerUserId").asText(json.path("user_id").asText(""));
      String amount = json.path("amount").asText("0");
      String reason = json.path("reason").asText("High-velocity activity detected");

      if (userIdStr.isEmpty()) return;
      UUID userId = UUID.fromString(userIdStr);

      notificationService.sendNotification(
        userId,
        NotificationType.SECURITY_ALERT,
        NotificationChannel.SMS,
        "Payment Held For Security Review",
        String.format(
          "Your payment of LKR %s has been held for review. Reason: %s",
          amount,
          reason
        ),
        null,
        message
      );
    } catch (Exception e) {
      log.warn("Failed to process payment held event for notification: {}", e.getMessage());
    }
  }
}
