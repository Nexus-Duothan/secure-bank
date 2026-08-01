package com.securebank.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.securebank.notification.OtpChallengeDeliveryRequest;
import com.securebank.notification.OtpChallengeDeliveryResponse;
import com.securebank.notification.dispatcher.NotificationDispatcher;
import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtpChallengeDeliveryServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  /** Records what it was asked to send so the test can assert on delivery. */
  private static final class RecordingDispatcher implements NotificationDispatcher {

    private final NotificationChannel channel;
    private String lastMessage;
    private String lastRecipient;

    private RecordingDispatcher(NotificationChannel channel) {
      this.channel = channel;
    }

    @Override
    public NotificationChannel getChannel() {
      return channel;
    }

    @Override
    public boolean dispatch(
      UUID userId,
      NotificationType type,
      String title,
      String message,
      String recipientContact
    ) {
      this.lastMessage = message;
      this.lastRecipient = recipientContact;
      return true;
    }
  }

  private static OtpChallengeDeliveryRequest request(
    String email,
    String phoneNumber,
    boolean emailEnabled,
    boolean smsEnabled
  ) {
    return new OtpChallengeDeliveryRequest(
      USER_ID,
      "Kaveesha Kapitiarachchi",
      email,
      phoneNumber,
      emailEnabled,
      smsEnabled,
      "OPEN_ACCOUNT",
      "123456",
      Instant.parse("2026-08-01T12:00:00Z")
    );
  }

  @Test
  @DisplayName("sends the code by SMS only when email delivery is switched off")
  void queuesOtpAsSmsOnly() {
    RecordingDispatcher sms = new RecordingDispatcher(NotificationChannel.SMS);
    RecordingDispatcher email = new RecordingDispatcher(NotificationChannel.EMAIL);
    OtpChallengeDeliveryService service = new OtpChallengeDeliveryService(List.of(sms, email));

    OtpChallengeDeliveryResponse response = service.queueOtpChallenge(
      request("kaveesha@securebank.lk", "+94 77 510 6101", false, true)
    );

    assertThat(response.status()).isEqualTo("queued");
    assertThat(response.channelsQueued()).containsExactly("sms");
    assertThat(sms.lastRecipient).isEqualTo("+94 77 510 6101");
    assertThat(sms.lastMessage).contains("123456");
    assertThat(email.lastMessage).isNull();
  }

  @Test
  @DisplayName("uses both channels when the customer has email and SMS enabled")
  void queuesOtpOnBothChannels() {
    RecordingDispatcher sms = new RecordingDispatcher(NotificationChannel.SMS);
    RecordingDispatcher email = new RecordingDispatcher(NotificationChannel.EMAIL);
    OtpChallengeDeliveryService service = new OtpChallengeDeliveryService(List.of(sms, email));

    OtpChallengeDeliveryResponse response = service.queueOtpChallenge(
      request("kaveesha@securebank.lk", "+94 77 510 6101", true, true)
    );

    assertThat(response.channelsQueued()).containsExactlyInAnyOrder("sms", "email");
    assertThat(email.lastRecipient).isEqualTo("kaveesha@securebank.lk");
  }

  @Test
  @DisplayName("reports not-queued when there is no contact detail to send to")
  void reportsNotQueuedWithoutContactDetails() {
    RecordingDispatcher sms = new RecordingDispatcher(NotificationChannel.SMS);
    OtpChallengeDeliveryService service = new OtpChallengeDeliveryService(List.of(sms));

    OtpChallengeDeliveryResponse response = service.queueOtpChallenge(
      request(null, "  ", true, true)
    );

    assertThat(response.status()).isEqualTo("not-queued");
    assertThat(response.channelsQueued()).isEmpty();
    assertThat(sms.lastMessage).isNull();
  }
}
