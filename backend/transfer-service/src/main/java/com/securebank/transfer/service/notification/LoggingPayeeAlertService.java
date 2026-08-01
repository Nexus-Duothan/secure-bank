package com.securebank.transfer.service.notification;

import com.securebank.transfer.entity.Payee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Logs the alert instead of delivering it. Real delivery is notification-service's job (FR-29);
 * that service is still a stub, so this is the same placeholder pattern used elsewhere in the
 * platform until it's wired up.
 */
@Service
@Slf4j
public class LoggingPayeeAlertService implements PayeeAlertService {

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
}
