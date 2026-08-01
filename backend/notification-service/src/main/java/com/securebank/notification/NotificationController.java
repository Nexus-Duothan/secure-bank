package com.securebank.notification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

  private static final List<NotificationResponse> NOTIFICATIONS = List.of(
    new NotificationResponse(
      "ntf-demo-001",
      "security",
      "New sign-in verified",
      "Chrome on Windows from Colombo, LK was approved using your primary mobile device.",
      "Security alert",
      "14:22",
      "Today",
      false
    ),
    new NotificationResponse(
      "ntf-demo-002",
      "security",
      "Card transaction protected",
      "A card attempt of LKR 92,000.00 from an unfamiliar location was stopped for your safety.",
      "Security alert",
      "09:47",
      "Today",
      false
    ),
    new NotificationResponse(
      "ntf-demo-003",
      "info",
      "Loan installment received",
      "Your July loan installment of LKR 24,350.00 was received successfully.",
      null,
      "05 Jul - 08:00",
      "Earlier",
      true
    ),
    new NotificationResponse(
      "ntf-demo-004",
      "info",
      "Statement available",
      "Your June statement for Everyday Current is ready to download.",
      null,
      "01 Jul - 06:12",
      "Earlier",
      true
    )
  );

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
      false
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

  @GetMapping("/api/v1/notifications/")
  public List<NotificationResponse> getNotifications() {
    return NOTIFICATIONS;
  }

  @PostMapping("/api/v1/notifications/mark-all-read")
  public Map<String, String> markAllRead() {
    return Map.of("status", "ok");
  }

  @GetMapping("/api/v1/audit/transactions")
  public List<AuditTransactionResponse> getAuditTransactions() {
    return AUDIT_TRANSACTIONS;
  }

  public record NotificationResponse(
    String id,
    String type,
    String title,
    String description,
    String categoryLabel,
    String timestamp,
    String groupLabel,
    boolean read
  ) {}

  public record AuditTransactionResponse(
    String id,
    String merchant,
    String category,
    String location,
    BigDecimal amount,
    String currency,
    String timestamp,
    String dateGroupLabel,
    String journalId,
    boolean flagged
  ) {}
}
