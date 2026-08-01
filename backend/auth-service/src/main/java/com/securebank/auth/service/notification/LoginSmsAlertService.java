package com.securebank.auth.service.notification;

public interface LoginSmsAlertService {
  void sendSuccessfulLoginAlert(
    String phoneNumber,
    String fullName,
    String ipAddress,
    String deviceInfo
  );
}
