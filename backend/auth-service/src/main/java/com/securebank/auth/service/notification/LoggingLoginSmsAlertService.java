package com.securebank.auth.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingLoginSmsAlertService implements LoginSmsAlertService {

  @Override
  public void sendSuccessfulLoginAlert(
    String phoneNumber,
    String fullName,
    String ipAddress,
    String deviceInfo
  ) {
    if (phoneNumber == null || phoneNumber.isBlank()) {
      log.warn("Skipping login SMS alert because no phone number is stored for {}", fullName);
      return;
    }

    log.info(
      "SMS login alert queued for {} at {}: SecureBank login detected for {} from {} ({})",
      phoneNumber,
      fullName,
      fullName,
      deviceInfo,
      ipAddress == null || ipAddress.isBlank() ? "unknown IP" : ipAddress
    );
  }
}
