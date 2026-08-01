package com.securebank.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationCenterServiceTest {

  @Test
  void queuesOtpAsSmsOnly() {
    NotificationCenterService service = new NotificationCenterService(
      new NotificationProperties(
        new NotificationProperties.Sms(
          "log",
          "SecureBank",
          new NotificationProperties.Twilio("", "", "")
        ),
        new NotificationProperties.Email("log", "no-reply@securebank.lk"),
        new NotificationProperties.InApp(true)
      ),
      null
    );

    OtpChallengeDeliveryResponse response = service.queueOtpChallenge(
      new OtpChallengeDeliveryRequest(
        UUID.randomUUID(),
        "Kaveesha Kapitiarachchi",
        "kaveesha.kapitiarachchi@securebank.lk",
        "+94 77 123 4567",
        true,
        true,
        "UPDATE_PROFILE",
        "904834",
        Instant.parse("2026-08-01T18:55:00Z")
      )
    );

    assertThat(response.status()).isEqualTo("queued");
    assertThat(response.channelsQueued()).containsExactly("sms");
    assertThat(response.notificationId()).isBlank();
  }

  @Test
  void marksAllNotificationsAsRead() {
    NotificationCenterService service = new NotificationCenterService(
      new NotificationProperties(
        new NotificationProperties.Sms(
          "log",
          "SecureBank",
          new NotificationProperties.Twilio("", "", "")
        ),
        new NotificationProperties.Email("log", "no-reply@securebank.lk"),
        new NotificationProperties.InApp(true)
      ),
      null
    );

    service.markAllAsRead();

    assertThat(service.getNotifications()).allMatch(NotificationResponse::read);
  }
}
