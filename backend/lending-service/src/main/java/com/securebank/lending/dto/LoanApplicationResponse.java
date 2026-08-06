package com.securebank.lending.dto;

import com.securebank.lending.entity.LoanApplication;
import com.securebank.lending.enums.ApplicationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanApplicationResponse(
  UUID id,
  UUID applicantUserId,
  String purpose,
  String currency,
  BigDecimal amount,
  int termMonths,
  BigDecimal annualInterestRate,
  String linkedAccountId,
  ApplicationStatus status,
  String rejectionReason,
  UUID loanId,
  Instant createdAt
) {
  public static LoanApplicationResponse from(LoanApplication application) {
    return new LoanApplicationResponse(
      application.getId(),
      application.getApplicantUserId(),
      application.getPurpose(),
      "LKR",
      application.getAmount(),
      application.getTermMonths(),
      application.getAnnualInterestRate(),
      application.getLinkedAccountId(),
      application.getStatus(),
      application.getRejectionReason(),
      application.getLoanId(),
      application.getCreatedAt()
    );
  }
}
