package com.securebank.notification.service;

import com.securebank.notification.AuditTransactionResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Read side of the append-only transaction journal (FR-30). Entries are never
 * updated or removed here; the journal is written by the core banking services.
 */
@Service
public class AuditJournalService {

  private static final List<AuditTransactionResponse> AUDIT_TRANSACTIONS = List.of(
    new AuditTransactionResponse(
      "txn-demo-001",
      "Ceylon Electricity Board",
      "Bill payment",
      "Kandy",
      new BigDecimal("-84.20"),
      "LKR",
      "2026-08-01T09:12:00",
      "Today - 01 Aug 2026",
      "J-94021",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-002",
      "Salary Deposit",
      "Income",
      "SecureBank Payroll",
      new BigDecimal("3200.00"),
      "LKR",
      "2026-07-31T08:45:00",
      "Yesterday - 31 Jul 2026",
      "J-94020",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-003",
      "Kumar's Grocers",
      "Card payment",
      "Kandy",
      new BigDecimal("-46.75"),
      "LKR",
      "2026-07-20T18:20:00",
      "20 Jul 2026",
      "J-93981",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-004",
      "Transfer to A. Silva",
      "Outgoing transfer",
      "SecureBank Transfer",
      new BigDecimal("-150.00"),
      "LKR",
      "2026-07-19T14:10:00",
      "19 Jul 2026",
      "J-93972",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-005",
      "Highway Fuel Stop",
      "Transport",
      "Kadawatha",
      new BigDecimal("-18.40"),
      "LKR",
      "2026-07-18T11:40:00",
      "18 Jul 2026",
      "J-93958",
      true
    ),
    new AuditTransactionResponse(
      "txn-demo-006",
      "Mobile Reload",
      "Bills",
      "Dialog",
      new BigDecimal("-12.00"),
      "LKR",
      "2026-07-17T20:05:00",
      "17 Jul 2026",
      "J-93940",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-007",
      "Rent Collection",
      "Income",
      "Standing order",
      new BigDecimal("950.00"),
      "LKR",
      "2026-07-15T07:15:00",
      "15 Jul 2026",
      "J-93901",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-008",
      "Pharmacy Care",
      "Health",
      "Kandy",
      new BigDecimal("-36.90"),
      "LKR",
      "2026-07-14T17:45:00",
      "14 Jul 2026",
      "J-93890",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-009",
      "Dialog Broadband",
      "Internet",
      "Online",
      new BigDecimal("-42.50"),
      "LKR",
      "2026-07-13T10:22:00",
      "13 Jul 2026",
      "J-93878",
      false
    ),
    new AuditTransactionResponse(
      "txn-demo-010",
      "Bookshop",
      "Education",
      "Peradeniya",
      new BigDecimal("-22.10"),
      "LKR",
      "2026-07-12T13:35:00",
      "12 Jul 2026",
      "J-93860",
      false
    )
  );

  public List<AuditTransactionResponse> getAuditTransactions() {
    return AUDIT_TRANSACTIONS;
  }
}
