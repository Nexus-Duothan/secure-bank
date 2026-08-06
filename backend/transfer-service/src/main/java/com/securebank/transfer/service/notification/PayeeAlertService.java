package com.securebank.transfer.service.notification;

import com.securebank.transfer.entity.Payee;

/** Security alert hook for payee management. */
public interface PayeeAlertService {
  void sendPayeeAddedAlert(Payee payee);
}
