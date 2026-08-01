package com.securebank.user.service.notification;

import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.ChangeRequestType;
import java.time.Instant;

public interface UserSecurityAlertService {
  void sendOtpChallenge(
    UserProfile profile,
    ChangeRequestType type,
    String otpCode,
    Instant expiresAt
  );

  void sendCriticalChangeAlert(UserProfile profile, ChangeRequestType type, String detail);
}
