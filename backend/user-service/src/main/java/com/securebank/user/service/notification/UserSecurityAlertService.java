package com.securebank.user.service.notification;

import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.ChangeRequestType;

public interface UserSecurityAlertService {
  void sendCriticalChangeAlert(UserProfile profile, ChangeRequestType type, String detail);
}
