package com.securebank.lending.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.securebank.lending.entity.LoanInstallment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AmortizationCalculatorTest {

  private final AmortizationCalculator calculator = new AmortizationCalculator();

  @Test
  void buildSchedule_producesOneInstallmentPerMonth_endingAtZeroBalance() {
    List<LoanInstallment> schedule = calculator.buildSchedule(
      UUID.randomUUID(),
      new BigDecimal("120000"),
      new BigDecimal("12"),
      12,
      Instant.parse("2026-01-01T00:00:00Z")
    );

    assertThat(schedule).hasSize(12);
    assertThat(schedule.get(11).getRemainingBalanceAfter()).isEqualByComparingTo("0.00");
    assertThat(schedule.get(0).getInstallmentNumber()).isEqualTo(1);
    assertThat(schedule.get(11).getInstallmentNumber()).isEqualTo(12);
  }

  @Test
  void buildSchedule_installmentTotalsSumToPrincipalPlusInterest() {
    BigDecimal principal = new BigDecimal("500000");
    List<LoanInstallment> schedule = calculator.buildSchedule(
      UUID.randomUUID(),
      principal,
      new BigDecimal("11.5"),
      24,
      Instant.parse("2026-01-01T00:00:00Z")
    );

    BigDecimal principalSum = schedule
      .stream()
      .map(LoanInstallment::getPrincipalAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    assertThat(principalSum).isEqualByComparingTo(principal);
  }

  @Test
  void buildSchedule_zeroInterestRate_splitsPrincipalEvenly() {
    List<LoanInstallment> schedule = calculator.buildSchedule(
      UUID.randomUUID(),
      new BigDecimal("100000"),
      BigDecimal.ZERO,
      10,
      Instant.parse("2026-01-01T00:00:00Z")
    );

    assertThat(schedule.get(0).getInterestAmount()).isEqualByComparingTo("0.00");
    assertThat(schedule.get(0).getPrincipalAmount()).isEqualByComparingTo("10000.00");
    assertThat(schedule.get(9).getRemainingBalanceAfter()).isEqualByComparingTo("0.00");
  }

  @Test
  void buildSchedule_dueDatesAdvanceOneMonthPerInstallment() {
    List<LoanInstallment> schedule = calculator.buildSchedule(
      UUID.randomUUID(),
      new BigDecimal("60000"),
      new BigDecimal("10"),
      3,
      Instant.parse("2026-01-15T00:00:00Z")
    );

    assertThat(schedule.get(0).getDueDate()).isEqualTo(Instant.parse("2026-02-15T00:00:00Z"));
    assertThat(schedule.get(1).getDueDate()).isEqualTo(Instant.parse("2026-03-15T00:00:00Z"));
    assertThat(schedule.get(2).getDueDate()).isEqualTo(Instant.parse("2026-04-15T00:00:00Z"));
  }
}
