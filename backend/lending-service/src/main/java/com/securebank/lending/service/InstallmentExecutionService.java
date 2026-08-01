package com.securebank.lending.service;

import com.securebank.lending.client.AccountSnapshot;
import com.securebank.lending.client.AccountsClient;
import com.securebank.lending.client.AccountsUnavailableException;
import com.securebank.lending.config.LendingServiceProperties;
import com.securebank.lending.entity.Loan;
import com.securebank.lending.entity.LoanInstallment;
import com.securebank.lending.enums.InstallmentStatus;
import com.securebank.lending.enums.LoanStatus;
import com.securebank.lending.exception.ResourceNotFoundException;
import com.securebank.lending.kafka.LoanEventProducer;
import com.securebank.lending.kafka.event.RepaymentOverdueEvent;
import com.securebank.lending.kafka.event.RepaymentReminderEvent;
import com.securebank.lending.repository.LoanInstallmentRepository;
import com.securebank.lending.repository.LoanRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that actually collects an installment (FR-25), shared by the scheduled
 * auto-deduct runner and the customer-facing "pay now" action so both go through identical
 * locking and retry-policy logic. Like transfer-service's AccountsClient, accounts-service has
 * no real debit endpoint yet — this only checks the linked account's balance and records the
 * outcome; it does not move real money. Once accounts-service grows a debit contract, only
 * the "collected" branch below needs to call it.
 */
@Service
@RequiredArgsConstructor
public class InstallmentExecutionService {

  private static final Logger log = LoggerFactory.getLogger(InstallmentExecutionService.class);

  private final LoanRepository loanRepository;
  private final LoanInstallmentRepository loanInstallmentRepository;
  private final AccountsClient accountsClient;
  private final LendingServiceProperties properties;
  private final LoanEventProducer loanEventProducer;

  @Transactional
  public LoanInstallment collectDue(UUID installmentId) {
    LoanInstallment installment = loanInstallmentRepository
      .findForUpdateById(installmentId)
      .orElse(null);
    if (installment == null || installment.getStatus() == InstallmentStatus.PAID) {
      // Already collected (e.g. by a manual pay that raced an overlapping runner tick), or
      // gone. Either way there's nothing left to do.
      return installment;
    }

    Loan loan = loanRepository
      .findForUpdateById(installment.getLoanId())
      .orElseThrow(() ->
        new ResourceNotFoundException("Loan not found: " + installment.getLoanId())
      );

    boolean collected = attemptCollection(loan, installment);

    if (collected) {
      installment.setStatus(InstallmentStatus.PAID);
      installment.setPaidAt(Instant.now());
      installment.setNextRetryAt(null);
      loanInstallmentRepository.save(installment);
      markLoanPaidOffIfComplete(loan);
    } else {
      recordFailedAttempt(loan, installment);
    }

    return installment;
  }

  private boolean attemptCollection(Loan loan, LoanInstallment installment) {
    try {
      AccountSnapshot account = accountsClient.getAccount(loan.getLinkedAccountId());
      return account.balance().compareTo(installment.getTotalAmount()) >= 0;
    } catch (AccountsUnavailableException ex) {
      // Treated the same as insufficient funds: a transient infra fault shouldn't be
      // distinguished from "couldn't collect this time" for retry purposes.
      log.warn(
        "accounts-service unreachable while collecting installment {}: {}",
        installment.getId(),
        ex.getMessage()
      );
      return false;
    }
  }

  private void recordFailedAttempt(Loan loan, LoanInstallment installment) {
    int attempts = installment.getFailedAttempts() + 1;
    installment.setFailedAttempts(attempts);

    LendingServiceProperties.Repayment policy = properties.repayment();
    if (attempts >= policy.maxAttempts()) {
      installment.setStatus(InstallmentStatus.OVERDUE);
      installment.setNextRetryAt(null);
      loanInstallmentRepository.save(installment);

      loan.setStatus(LoanStatus.DELINQUENT);
      loanRepository.save(loan);

      loanEventProducer.publishOverdue(
        new RepaymentOverdueEvent(
          loan.getId(),
          loan.getBorrowerUserId(),
          installment.getId(),
          installment.getInstallmentNumber(),
          installment.getTotalAmount(),
          installment.getDueDate(),
          Instant.now()
        )
      );
    } else {
      installment.setStatus(InstallmentStatus.FAILED);
      installment.setNextRetryAt(Instant.now().plus(policy.retryInterval()));
      loanInstallmentRepository.save(installment);
    }
  }

  private void markLoanPaidOffIfComplete(Loan loan) {
    boolean hasOutstanding = loanInstallmentRepository
      .findFirstByLoanIdAndStatusInOrderByInstallmentNumberAsc(
        loan.getId(),
        List.of(InstallmentStatus.PENDING, InstallmentStatus.FAILED, InstallmentStatus.OVERDUE)
      )
      .isPresent();

    if (!hasOutstanding) {
      loan.setStatus(LoanStatus.PAID_OFF);
      loanRepository.save(loan);
    }
  }

  /**
   * FR-26. Not pessimistic-locked: this only sets a single "already reminded" marker rather
   * than moving money, so at worst a rare overlapping tick sends one duplicate reminder -
   * acceptable under NFR-R4's at-least-once delivery model, not worth a lock for.
   */
  @Transactional
  public void sendReminder(UUID installmentId) {
    LoanInstallment installment = loanInstallmentRepository.findById(installmentId).orElse(null);
    if (installment == null || installment.getReminderSentAt() != null) {
      return;
    }

    Loan loan = loanRepository.findById(installment.getLoanId()).orElse(null);
    if (loan == null) {
      return;
    }

    installment.setReminderSentAt(Instant.now());
    loanInstallmentRepository.save(installment);

    loanEventProducer.publishReminder(
      new RepaymentReminderEvent(
        loan.getId(),
        loan.getBorrowerUserId(),
        installment.getId(),
        installment.getInstallmentNumber(),
        installment.getTotalAmount(),
        installment.getDueDate(),
        Instant.now()
      )
    );
  }
}
