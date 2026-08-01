package com.securebank.user.service.notification;

import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.ChangeRequestType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class LoggingUserSecurityAlertService implements UserSecurityAlertService {

  private static final DateTimeFormatter ALERT_TIME = DateTimeFormatter.ofPattern(
    "dd MMM yyyy HH:mm"
  ).withZone(ZoneId.of("Asia/Colombo"));
  private final RestClient restClient;

  public LoggingUserSecurityAlertService(
    @Value(
      "${securebank.notification.service-url:http://localhost:8088}"
    ) String notificationServiceUrl
  ) {
    this.restClient = RestClient.builder().baseUrl(notificationServiceUrl).build();
  }

  @Override
  public void sendOtpChallenge(
    UserProfile profile,
    ChangeRequestType type,
    String otpCode,
    Instant expiresAt
  ) {
    OtpChallengeDispatchRequest request = new OtpChallengeDispatchRequest(
      profile.getId(),
      profile.getFullName(),
      profile.getEmail(),
      profile.getPhoneNumber(),
      profile.isEmailNotifications(),
      profile.isSmsNotifications(),
      type.name(),
      otpCode,
      expiresAt
    );

    try {
      restClient
        .post()
        .uri("/api/v1/notifications/otp-challenges")
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .toBodilessEntity();
      log.info(
        "OTP challenge queued for {} via notification-service on {}.",
        profile.getFullName(),
        ALERT_TIME.format(Instant.now())
      );
    } catch (RuntimeException exception) {
      log.warn(
        "Unable to queue OTP challenge via notification-service for {}. {}",
        profile.getFullName(),
        exception.getMessage()
      );
    }
  }

  @Override
  public void sendCriticalChangeAlert(UserProfile profile, ChangeRequestType type, String detail) {
    String phone =
      profile.getPhoneNumber() == null || profile.getPhoneNumber().isBlank()
        ? "no-phone-on-file"
        : profile.getPhoneNumber();
    String email =
      profile.getEmail() == null || profile.getEmail().isBlank()
        ? "no-email-on-file"
        : profile.getEmail();

    log.info(
      "Critical security alert queued via SMS and email for {} / {}: {} for {} on {}. {}",
      phone,
      email,
      toAlertTitle(type),
      profile.getFullName(),
      ALERT_TIME.format(Instant.now()),
      detail
    );
  }

  private String toAlertTitle(ChangeRequestType type) {
    return switch (type) {
      case UPDATE_PROFILE -> "Personal details updated";
      case LINK_DEVICE -> "New device linking requested";
      case TRUST_DEVICE -> "New device linked";
      case REVOKE_DEVICE -> "Linked device revoked";
      case UPDATE_NOTIFICATION_PREFERENCES -> "Notification preferences updated";
      case ADMIN_UPDATE_ROLE -> "Account role changed by bank staff";
      case ADMIN_UPDATE_STATUS -> "Account status changed by bank staff";
    };
  }
}
