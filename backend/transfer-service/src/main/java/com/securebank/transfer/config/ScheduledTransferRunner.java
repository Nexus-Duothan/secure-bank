package com.securebank.transfer.config;

import com.securebank.transfer.enums.ScheduleStatus;
import com.securebank.transfer.repository.ScheduledTransferRepository;
import com.securebank.transfer.service.ScheduledTransferExecutionService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for due scheduled payments and hands each off to
 * {@link ScheduledTransferExecutionService} for a locked, transactional execution. This method
 * itself stays un-transactional and the poll query is a cheap non-locking read. Business failures
 * inside a single schedule's execution are caught and recorded by
 * {@link ScheduledTransferExecutionService#executeDue}; an unexpected exception escaping that call
 * (e.g. the lock read or final save failing) is caught here too, so one bad schedule can't abort
 * the rest of this tick's loop. Worst case that schedule is simply retried on the next tick.
 */
@Component
@RequiredArgsConstructor
public class ScheduledTransferRunner {

  private static final Logger log = LoggerFactory.getLogger(ScheduledTransferRunner.class);

  private final ScheduledTransferRepository scheduledTransferRepository;
  private final ScheduledTransferExecutionService executionService;

  @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
  public void runDueSchedules() {
    for (UUID scheduleId : scheduledTransferRepository.findDueIds(
      ScheduleStatus.ACTIVE,
      Instant.now()
    )) {
      try {
        executionService.executeDue(scheduleId);
      } catch (RuntimeException exception) {
        log.error(
          "Unexpected failure executing scheduled transfer {}; will retry next tick",
          scheduleId,
          exception
        );
      }
    }
  }
}
