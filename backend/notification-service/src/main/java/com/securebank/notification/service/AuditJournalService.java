package com.securebank.notification.service;

import com.securebank.notification.AuditTransactionResponse;
import com.securebank.notification.client.LedgerJournalClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Read side of the append-only transaction journal (FR-30). The journal itself is the ledger in
 * accounts-service, written by the core banking services; nothing is stored or invented here, so a
 * bank with no posted movements yet correctly shows an empty audit list.
 */
@Service
@RequiredArgsConstructor
public class AuditJournalService {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
  private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd MMM yyyy");
  private static final int JOURNAL_PAGE_SIZE = 200;

  private final LedgerJournalClient ledgerJournalClient;

  public List<AuditTransactionResponse> getAuditTransactions() {
    return ledgerJournalClient
      .getJournal(JOURNAL_PAGE_SIZE)
      .stream()
      .map(entry -> {
        LocalDateTime localTimestamp = LocalDateTime.ofInstant(entry.timestamp(), COLOMBO);
        return new AuditTransactionResponse(
          entry.id(),
          entry.merchant(),
          entry.category(),
          entry.location(),
          entry.amount(),
          entry.currency(),
          localTimestamp.toString(),
          dateGroupLabel(localTimestamp.toLocalDate()),
          entry.journalId(),
          entry.flagged()
        );
      })
      .toList();
  }

  private String dateGroupLabel(LocalDate date) {
    LocalDate today = LocalDate.now(COLOMBO);
    String formatted = DATE_LABEL.format(date);
    if (date.equals(today)) {
      return "Today - " + formatted;
    }
    if (date.equals(today.minusDays(1))) {
      return "Yesterday - " + formatted;
    }
    return formatted;
  }
}
