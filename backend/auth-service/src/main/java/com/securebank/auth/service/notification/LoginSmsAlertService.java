package com.securebank.auth.service.notification;

import java.time.Instant;
import java.util.UUID;

public interface LoginSmsAlertService {
  void sendSuccessfulLoginAlert(
    String phoneNumber,
    String fullName,
    String ipAddress,
    String deviceInfo,
    Instant occurredAt
  );

  void sendPasswordChangedAlert(
    String phoneNumber,
    String email,
    String fullName,
    Instant occurredAt
  );

  void sendRegistrationOtpChallenge(
    UUID userId,
    String fullName,
    String phoneNumber,
    String email,
    String code
  );
}
