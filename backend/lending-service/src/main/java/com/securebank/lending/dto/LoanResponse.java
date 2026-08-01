package com.securebank.lending.dto;

import com.securebank.lending.enums.LoanStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LoanResponse(
  UUID id,
  UUID applicationId,
  String purpose,
  BigDecimal principal,
  BigDecimal annualInterestRate,
  int termMonths,
  String currency,
  String linkedAccountId,
  LoanStatus status,
  boolean autopayEnabled,
  BigDecimal remainingBalance,
  int installmentsPaid,
  int installmentsTotal,
  Instant nextInstallmentDueDate,
  BigDecimal nextInstallmentAmount,
  Instant disbursedAt
) {}
