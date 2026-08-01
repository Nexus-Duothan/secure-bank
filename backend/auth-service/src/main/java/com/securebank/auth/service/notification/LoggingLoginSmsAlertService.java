package com.securebank.auth.service.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingLoginSmsAlertService implements LoginSmsAlertService {

  private static final DateTimeFormatter ALERT_TIME = DateTimeFormatter.ofPattern(
    "dd MMM yyyy HH:mm"
  ).withZone(ZoneId.of("Asia/Colombo"));

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
}
