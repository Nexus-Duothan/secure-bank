package com.securebank.lending.config;

import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalised tuning for the Lending service (see {@code application.yml}). */
@ConfigurationProperties(prefix = "securebank.lending")
public record LendingServiceProperties(
  AccountsClient accountsClient,
  Loan loan,
  Repayment repayment
) {
  public LendingServiceProperties {
    accountsClient = accountsClient == null ? new AccountsClient(null) : accountsClient;
    loan = loan == null ? new Loan(null, null, 0, 0, null) : loan;
    repayment = repayment == null ? new Repayment(0, null, null) : repayment;
  }

  public record AccountsClient(String baseUrl) {
    public AccountsClient {
      baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8084" : baseUrl;
    }
  }

  /**
   * @param minAmount smallest amount a loan application may request
   * @param maxAmount largest amount a loan application may request
   * @param minTermMonths shortest repayment term offered
   * @param maxTermMonths longest repayment term offered
   * @param defaultAnnualInterestRate flat estimated/offered annual rate (percent) applied at
   *     application time; the platform has no risk-based pricing model yet
   */
  public record Loan(
    BigDecimal minAmount,
    BigDecimal maxAmount,
    int minTermMonths,
    int maxTermMonths,
    BigDecimal defaultAnnualInterestRate
  ) {
    public Loan {
      minAmount = minAmount == null ? new BigDecimal("10000") : minAmount;
      maxAmount = maxAmount == null ? new BigDecimal("5000000") : maxAmount;
      minTermMonths = minTermMonths <= 0 ? 6 : minTermMonths;
      maxTermMonths = maxTermMonths <= 0 ? 60 : maxTermMonths;
      defaultAnnualInterestRate =
        defaultAnnualInterestRate == null ? new BigDecimal("11.5") : defaultAnnualInterestRate;
    }
  }

  /**
   * The automated-repayment retry policy, which is a documented judgment call rather than a
   * given: on insufficient funds (or accounts-service being unreachable — both are "couldn't
   * collect this time" outcomes), retry once per day for
   * {@code maxAttempts} days before giving up on that installment and marking it OVERDUE
   * (which also flips the loan to DELINQUENT). {@code reminderLeadTime} governs the reminder:
   * how long before an installment's due date a reminder event is published.
   */
  public record Repayment(int maxAttempts, Duration retryInterval, Duration reminderLeadTime) {
    public Repayment {
      maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
      retryInterval = retryInterval == null ? Duration.ofDays(1) : retryInterval;
      reminderLeadTime = reminderLeadTime == null ? Duration.ofDays(3) : reminderLeadTime;
    }
  }
}
