package com.securebank.transfer.service.notification;

import com.securebank.transfer.entity.Payee;

/** Security alert hook for new payee additions (FR-28). */
public interface PayeeAlertService {
  void sendPayeeAddedAlert(Payee payee);
}
