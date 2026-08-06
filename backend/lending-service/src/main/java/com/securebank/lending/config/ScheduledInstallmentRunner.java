package com.securebank.lending.config;

import com.securebank.lending.repository.LoanInstallmentRepository;
import com.securebank.lending.service.InstallmentExecutionService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for due installment collections and reminders, handing each off
 * individually to {@link InstallmentExecutionService} for a locked, transactional execution.
 * This method itself stays un-transactional and the poll queries are cheap non-locking reads,
 * so one slow or failing installment can't hold up the others in the same tick — though, like
 * transfer-service's equivalent runner, that guarantee only covers business-logic failures
 * inside the execution service; an exception from the lock acquisition or final save would
 * still abort the rest of that tick's loop and simply retry on the next one.
 */
@Component
@RequiredArgsConstructor
public class ScheduledInstallmentRunner {

  private final LoanInstallmentRepository loanInstallmentRepository;
  private final LendingServiceProperties properties;
  private final InstallmentExecutionService executionService;

  @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
  public void collectDueInstallments() {
    for (UUID installmentId : loanInstallmentRepository.findDueForCollectionIds(Instant.now())) {
      executionService.collectDue(installmentId);
    }
  }

  @Scheduled(fixedDelayString = "PT15M", initialDelayString = "PT1M")
  public void sendDueReminders() {
    Instant remindBefore = Instant.now().plus(properties.repayment().reminderLeadTime());
    for (UUID installmentId : loanInstallmentRepository.findDueForReminderIds(remindBefore)) {
      executionService.sendReminder(installmentId);
    }
  }
}
