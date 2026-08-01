package com.securebank.lending.util;

import com.securebank.lending.entity.LoanInstallment;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Builds a standard reducing-balance (EMI) repayment schedule and persists every installment
 * up front at disbursement time (FR-24), rather than recomputing it on the fly.
 */
@Component
public class AmortizationCalculator {

  // No per-user timezone data exists yet; matches the fixed zone already used for schedule
  // advancement elsewhere in the platform (transfer-service's ScheduledTransferExecutionService).
  private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Colombo");
  private static final int MONEY_SCALE = 2;
  private static final MathContext RATE_MATH_CONTEXT = new MathContext(20);

  public List<LoanInstallment> buildSchedule(
    UUID loanId,
    BigDecimal principal,
    BigDecimal annualInterestRatePercent,
    int termMonths,
    Instant disbursedAt
  ) {
    BigDecimal monthlyRate = annualInterestRatePercent.divide(
      BigDecimal.valueOf(1200),
      RATE_MATH_CONTEXT
    );
    BigDecimal installmentAmount = calculateEmi(principal, monthlyRate, termMonths);

    List<LoanInstallment> installments = new ArrayList<>(termMonths);
    BigDecimal remaining = principal;

    for (int number = 1; number <= termMonths; number++) {
      boolean isLastInstallment = number == termMonths;

      BigDecimal interestForMonth = remaining
        .multiply(monthlyRate)
        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
      BigDecimal principalForMonth = isLastInstallment
        ? remaining
        : installmentAmount.subtract(interestForMonth).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
      BigDecimal totalForMonth = principalForMonth.add(interestForMonth);

      remaining = remaining.subtract(principalForMonth);
      if (isLastInstallment) {
        // Absorbs accumulated rounding residue so the schedule always lands exactly on zero.
        remaining = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
      }

      installments.add(
        LoanInstallment.builder()
          .loanId(loanId)
          .installmentNumber(number)
          .dueDate(disbursedAt.atZone(SCHEDULE_ZONE).plusMonths(number).toInstant())
          .principalAmount(principalForMonth)
          .interestAmount(interestForMonth)
          .totalAmount(totalForMonth)
          .remainingBalanceAfter(remaining)
          .build()
      );
    }

    return installments;
  }

  /** Standard EMI formula; falls back to a flat split when the rate is zero (avoids a division by zero). */
  private BigDecimal calculateEmi(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
    if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
      return principal.divide(BigDecimal.valueOf(termMonths), MONEY_SCALE, RoundingMode.HALF_UP);
    }

    BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
    BigDecimal onePlusRToN = onePlusR.pow(termMonths, RATE_MATH_CONTEXT);
    BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusRToN);
    BigDecimal denominator = onePlusRToN.subtract(BigDecimal.ONE);

    return numerator.divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
  }
}
