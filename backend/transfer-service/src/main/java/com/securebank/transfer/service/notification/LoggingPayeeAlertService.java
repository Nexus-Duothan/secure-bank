package com.securebank.transfer.service.notification;

import com.securebank.transfer.entity.Payee;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
