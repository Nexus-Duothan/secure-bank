package com.securebank.transfer.service.notification;

import com.securebank.transfer.entity.Payee;
import java.time.Instant;
import java.util.UUID;

/** Security alert and OTP delivery hook for payee management (FR-04, FR-28). */
public interface PayeeAlertService {
  void sendPayeeAddedAlert(Payee payee);
  void sendOtpChallenge(UUID ownerUserId, String changeType, String code, Instant expiresAt);
}
