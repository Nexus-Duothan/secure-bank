package com.securebank.lending;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Officer-side view of the loan queue (FR-22/FR-23). Read-only by design: the
 * approve/reject decisions are made in the bank's own lending system, not in
 * this app. The API Gateway stamps X-User-Role from the verified JWT, so the
 * role header is trustworthy for calls that came through the gateway.
 */
@RestController
@RequestMapping("/api/v1/loans/officer")
public class OfficerLendingController {

  private static final List<PendingLoanApplication> PENDING_APPLICATIONS = List.of(
    new PendingLoanApplication(
      "loan-app-1001",
      "Kaveesha Kapitiarachchi",
      "usr-demo-001",
      "Home improvement",
      "LKR",
      new BigDecimal("450000.00"),
      24,
      11.5,
      "UNDER_REVIEW",
      "2026-08-01T07:15:00Z"
    ),
    new PendingLoanApplication(
      "loan-app-1002",
      "Kumar's Grocers",
      "usr-demo-003",
      "Small business stock",
      "LKR",
      new BigDecimal("850000.00"),
      36,
      12.25,
      "UNDER_REVIEW",
      "2026-07-30T13:48:00Z"
    )
  );

  @GetMapping("/pending")
  public List<PendingLoanApplication> getPendingApplications(
    @RequestHeader(value = "X-User-Role", required = false) String role
  ) {
    requireOfficer(role);
    return PENDING_APPLICATIONS;
  }

  private void requireOfficer(String role) {
    if (!"BANK_OFFICER".equals(role) && !"ADMIN".equals(role)) {
      throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Only bank officers may view the loan queue"
      );
    }
  }

  public record PendingLoanApplication(
    String id,
    String applicantName,
    String applicantId,
    String purpose,
    String currency,
    BigDecimal amount,
    int termMonths,
    double estimatedRate,
    String status,
    String submittedAt
  ) {}
}
