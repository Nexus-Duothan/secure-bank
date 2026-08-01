package com.securebank.transfer.service.notification;

import com.securebank.transfer.entity.Payee;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class LoggingPayeeAlertService implements PayeeAlertService {

  private final RestClient restClient;

  public LoggingPayeeAlertService(
    @Value("${notification-service.url:http://localhost:8088}") String notificationServiceUrl
  ) {
    this.restClient = RestClient.builder().baseUrl(notificationServiceUrl).build();
  }

  @Override
  public void sendPayeeAddedAlert(Payee payee) {
    log.info(
      "Security alert queued for user {}: new payee '{}' ({}) added, 12h cooling-off until {}",
      payee.getOwnerUserId(),
      payee.getNickname(),
      payee.getAccountReference(),
      payee.getCoolingOffUntil()
    );
  }

  @Override
  public void sendOtpChallenge(
    UUID ownerUserId,
    String changeType,
    String code,
    Instant expiresAt
  ) {
    log.info(
      "Dispatching OTP challenge code [{}] for user {} ({}) expiring at {}",
      code,
      ownerUserId,
      changeType,
      expiresAt
    );
    try {
      restClient
        .post()
        .uri("/api/v1/notifications/otp-challenges")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          Map.of(
            "userId",
            ownerUserId.toString(),
            "fullName",
            "SecureBank Customer",
            "emailEnabled",
            true,
            "smsEnabled",
            true,
            "changeType",
            changeType,
            "otpCode",
            code,
            "expiresAt",
            expiresAt.toString()
          )
        )
        .retrieve()
        .toBodilessEntity();
    } catch (Exception exception) {
      log.warn(
        "Unable to dispatch OTP challenge via notification-service (fallback to log-only): {}",
        exception.getMessage()
      );
    }
  }
}
