package com.securebank.lending;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loans")
public class LendingController {

  @PostMapping("/apply")
  public LoanApplicationResponse applyForLoan(@RequestBody LoanApplicationRequest request) {
    return new LoanApplicationResponse(
      "loan-" + UUID.randomUUID(),
      "UNDER_REVIEW",
      11.5,
      OffsetDateTime.now().toString()
    );
  }

  @GetMapping("/{id}")
  public LoanDetailResponse getLoan(@PathVariable String id) {
    return new LoanDetailResponse(
      id,
      "Home Improvement Loan",
      "LKR",
      new BigDecimal("318420.00"),
      9,
      24,
      "05 Aug 2026",
      new BigDecimal("24350.00"),
      true,
      "Everyday Current"
    );
  }

  public record LoanApplicationRequest(String purpose, BigDecimal amount, int termMonths) {}

  public record LoanApplicationResponse(
    String id,
    String status,
    double estimatedRate,
    String createdAt
  ) {}

  public record LoanDetailResponse(
    String id,
    String name,
    String currency,
    BigDecimal remainingBalance,
    int installmentsPaid,
    int installmentsTotal,
    String nextPaymentDueDate,
    BigDecimal nextPaymentAmount,
    boolean autoPayEnabled,
    String autoPayAccountName
  ) {}
}
