package com.securebank.notification.controller;

import com.securebank.notification.AuditTransactionResponse;
import com.securebank.notification.service.AuditJournalService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only view of the transaction journal (FR-30), used by the admin audit page
 * and the officer's flagged-transaction queue.
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

  private final AuditJournalService auditJournalService;

  @GetMapping("/transactions")
  public List<AuditTransactionResponse> getAuditTransactions() {
    return auditJournalService.getAuditTransactions();
  }
}
