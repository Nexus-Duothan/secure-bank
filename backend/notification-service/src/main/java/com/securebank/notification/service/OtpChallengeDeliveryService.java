package com.securebank.notification.service;

import com.securebank.notification.OtpChallengeDeliveryRequest;
import com.securebank.notification.OtpChallengeDeliveryResponse;
import com.securebank.notification.PasswordResetDeliveryRequest;
import com.securebank.notification.dispatcher.NotificationDispatcher;
import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Relays one-time codes raised by the other services (accounts, user) out to the
 * customer over SMS and email (FR-04, FR-29).
 *
 * <p>The code is delivered straight through the channel dispatchers rather than
 * {@link MultiChannelNotificationService}: an OTP is a transient credential, so it is
 * deliberately never written to the notification table.
 */
@Service
@Slf4j
public class OtpChallengeDeliveryService {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
  private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern(
    "dd MMM yyyy HH:mm"
  ).withZone(COLOMBO);

  private final Map<NotificationChannel, NotificationDispatcher> dispatchers = new EnumMap<>(
    NotificationChannel.class
  );

  public OtpChallengeDeliveryService(List<NotificationDispatcher> dispatcherList) {
    for (NotificationDispatcher dispatcher : dispatcherList) {
      this.dispatchers.put(dispatcher.getChannel(), dispatcher);
    }
  }

  public OtpChallengeDeliveryResponse queueOtpChallenge(OtpChallengeDeliveryRequest request) {
    String title = "SecureBank verification code";
    String message =
      "SecureBank verification code: " +
      request.otpCode() +
      ". It expires at " +
      EXPIRY_FORMAT.format(request.expiresAt()) +
      ".";

    List<String> queued = new ArrayList<>();
    if (request.smsEnabled() && hasText(request.phoneNumber())) {
      dispatch(NotificationChannel.SMS, request, title, message, request.phoneNumber(), queued);
    }
    if (request.emailEnabled() && hasText(request.email())) {
      dispatch(NotificationChannel.EMAIL, request, title, message, request.email(), queued);
    }

    if (queued.isEmpty()) {
      log.warn(
        "No delivery channel available for {} OTP challenge of user {}",
        request.changeType(),
        request.userId()
      );
      return new OtpChallengeDeliveryResponse("not-queued", List.of(), "");
    }
    return new OtpChallengeDeliveryResponse("queued", List.copyOf(queued), "");
  }

  public OtpChallengeDeliveryResponse queuePasswordResetLink(PasswordResetDeliveryRequest request) {
    String title = "SecureBank password update link";
    String message =
      "Hello " +
      request.fullName() +
      ", use this secure link to update your SecureBank password: " +
      request.resetUrl() +
      ". The link expires at " +
      EXPIRY_FORMAT.format(request.expiresAt()) +
      ".";

    List<String> queued = new ArrayList<>();
    dispatch(NotificationChannel.EMAIL, request.userId(), title, message, request.email(), queued);

    if (queued.isEmpty()) {
      log.warn(
        "No delivery channel available for password reset link of user {}",
        request.userId()
      );
      return new OtpChallengeDeliveryResponse("not-queued", List.of(), "");
    }
    return new OtpChallengeDeliveryResponse("queued", List.copyOf(queued), "");
  }

  private void dispatch(
    NotificationChannel channel,
    OtpChallengeDeliveryRequest request,
    String title,
    String message,
    String recipient,
    List<String> queued
  ) {
    NotificationDispatcher dispatcher = dispatchers.get(channel);
    if (dispatcher == null) {
      return;
    }
    boolean delivered = dispatcher.dispatch(
      request.userId(),
      NotificationType.SECURITY_ALERT,
      title,
      message,
      recipient.trim()
    );
    if (delivered) {
      queued.add(channel.name().toLowerCase());
    }
  }

  private void dispatch(
    NotificationChannel channel,
    java.util.UUID userId,
    String title,
    String message,
    String recipient,
    List<String> queued
  ) {
    NotificationDispatcher dispatcher = dispatchers.get(channel);
    if (dispatcher == null || !hasText(recipient)) {
      return;
    }
    boolean delivered = dispatcher.dispatch(
      userId,
      NotificationType.SECURITY_ALERT,
      title,
      message,
      recipient.trim()
    );
    if (delivered) {
      queued.add(channel.name().toLowerCase());
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
