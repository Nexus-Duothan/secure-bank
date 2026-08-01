package com.securebank.lending.dto;

import com.securebank.lending.entity.LoanInstallment;
import com.securebank.lending.enums.InstallmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LoanInstallmentResponse(
  UUID id,
  int installmentNumber,
  Instant dueDate,
  BigDecimal principalAmount,
  BigDecimal interestAmount,
  BigDecimal totalAmount,
  BigDecimal remainingBalanceAfter,
  InstallmentStatus status,
  Instant paidAt
) {
  public static LoanInstallmentResponse from(LoanInstallment installment) {
    return new LoanInstallmentResponse(
      installment.getId(),
      installment.getInstallmentNumber(),
      installment.getDueDate(),
      installment.getPrincipalAmount(),
      installment.getInterestAmount(),
      installment.getTotalAmount(),
      installment.getRemainingBalanceAfter(),
      installment.getStatus(),
      installment.getPaidAt()
    );
  }
}
