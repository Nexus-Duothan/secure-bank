package com.securebank.user.service.notification;

import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.ChangeRequestType;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingUserSecurityAlertService implements UserSecurityAlertService {

  private static final DateTimeFormatter ALERT_TIME = DateTimeFormatter.ofPattern(
    "dd MMM yyyy HH:mm"
  ).withZone(ZoneId.of("Asia/Colombo"));

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
