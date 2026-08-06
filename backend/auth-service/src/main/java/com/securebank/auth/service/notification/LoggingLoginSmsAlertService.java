package com.securebank.auth.service.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class LoggingLoginSmsAlertService implements LoginSmsAlertService {

  private static final DateTimeFormatter ALERT_TIME = DateTimeFormatter.ofPattern(
    "dd MMM yyyy HH:mm"
  ).withZone(ZoneId.of("Asia/Colombo"));

  private final RestClient restClient;

  public LoggingLoginSmsAlertService(
    @Value("${notification-service.url:http://localhost:8088}") String notificationServiceUrl
  ) {
    this.restClient = RestClient.builder().baseUrl(notificationServiceUrl).build();
  }

  @Override
  public void sendSuccessfulLoginAlert(
    String phoneNumber,
    String fullName,
    String ipAddress,
    String deviceInfo,
    Instant occurredAt
  ) {
    if (phoneNumber == null || phoneNumber.isBlank()) {
      log.warn("Skipping login SMS alert because no phone number is stored for {}", fullName);
      return;
    }

    log.info(
      "SMS login alert queued for {}: SecureBank sign-in for {} on {}. Device: {}. Location reference: {}.",
      phoneNumber,
      fullName,
      ALERT_TIME.format(occurredAt),
      deviceInfo,
      ipAddress == null || ipAddress.isBlank() ? "unknown IP" : ipAddress
    );
  }

  @Override
  public void sendPasswordChangedAlert(
    String phoneNumber,
    String email,
    String fullName,
    Instant occurredAt
  ) {
    log.info(
      "Critical security alert queued via SMS and email for {} / {}: Password changed for {} on {}.",
      phoneNumber == null || phoneNumber.isBlank() ? "no-phone-on-file" : phoneNumber,
      email == null || email.isBlank() ? "no-email-on-file" : email,
      fullName,
      ALERT_TIME.format(occurredAt)
    );
  }

  @Override
  public void sendPasswordResetLink(
    UUID userId,
    String email,
    String fullName,
    String resetUrl,
    Instant expiresAt
  ) {
    log.info(
      "Dispatching password update link for user {} ({}) to email {}",
      userId,
      fullName,
      email
    );
    try {
      restClient
        .post()
        .uri("/api/v1/notifications/password-reset-links")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          Map.of(
            "userId",
            userId.toString(),
            "fullName",
            fullName != null ? fullName : "SecureBank Customer",
            "email",
            email != null ? email : "",
            "resetUrl",
            resetUrl,
            "expiresAt",
            expiresAt.toString()
          )
        )
        .retrieve()
        .toBodilessEntity();
    } catch (Exception exception) {
      log.warn(
        "Unable to dispatch password update link via notification-service (fallback to log-only): {}",
        exception.getMessage()
      );
    }
  }

  @Override
  public void sendRegistrationOtpChallenge(
    UUID userId,
    String fullName,
    String phoneNumber,
    String email,
    String code
  ) {
    log.info(
      "Dispatching registration OTP challenge [{}] for user {} ({}) to phone {} / email {}",
      code,
      userId,
      fullName,
      phoneNumber,
      email
    );
    try {
      restClient
        .post()
        .uri("/api/v1/notifications/otp-challenges")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          Map.of(
            "userId",
            userId.toString(),
            "fullName",
            fullName != null ? fullName : "SecureBank Customer",
            "phoneNumber",
            phoneNumber != null ? phoneNumber : "",
            "email",
            email != null ? email : "",
            "emailEnabled",
            true,
            "smsEnabled",
            true,
            "changeType",
            "REGISTRATION_MFA",
            "otpCode",
            code,
            "expiresAt",
            Instant.now().plusSeconds(300).toString()
          )
        )
        .retrieve()
        .toBodilessEntity();
    } catch (Exception exception) {
      log.warn(
        "Unable to dispatch registration OTP challenge via notification-service (fallback to log-only): {}",
        exception.getMessage()
      );
    }
  }
}
