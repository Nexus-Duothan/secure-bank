package com.securebank.transfer.config;

import com.securebank.transfer.enums.ScheduleStatus;
import com.securebank.transfer.repository.ScheduledTransferRepository;
import com.securebank.transfer.service.ScheduledTransferExecutionService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls for due scheduled payments (FR-19) and hands each off to
 * {@link ScheduledTransferExecutionService} for a locked, transactional execution. This method
 * itself stays un-transactional and the poll query is a cheap non-locking read, so one slow or
 * failing schedule can't hold up or roll back the others in the same tick.
 */
@Component
@RequiredArgsConstructor
public class ScheduledTransferRunner {

  private final ScheduledTransferRepository scheduledTransferRepository;
  private final ScheduledTransferExecutionService executionService;

  @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT30S")
  public void runDueSchedules() {
    for (UUID scheduleId : scheduledTransferRepository.findDueIds(ScheduleStatus.ACTIVE, Instant.now())) {
      executionService.executeDue(scheduleId);
    }
  }
}
