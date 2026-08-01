package com.securebank.transfer.service;

import com.securebank.transfer.dto.TransferQuoteRequest;
import com.securebank.transfer.dto.TransferResponse;
import com.securebank.transfer.entity.ScheduledTransfer;
import com.securebank.transfer.enums.ScheduleFrequency;
import com.securebank.transfer.enums.ScheduleStatus;
import com.securebank.transfer.enums.TransferStatus;
import com.securebank.transfer.repository.ScheduledTransferRepository;
import com.securebank.transfer.security.CallerIdentity;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Executes a single due scheduled payment (FR-19) by driving the same quote-then-confirm path a
 * user would go through, so scheduled runs get the exact same balance, limit and payee
 * cooling-off checks as an interactive transfer.
 */
@Service
@RequiredArgsConstructor
public class ScheduledTransferExecutionService {

  private static final Logger log = LoggerFactory.getLogger(
    ScheduledTransferExecutionService.class
  );
  // No per-user timezone data exists yet; matches the fixed zone already used for alert timestamps
  // elsewhere in the platform (e.g. LoggingUserSecurityAlertService).
  private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Colombo");

  private final ScheduledTransferRepository scheduledTransferRepository;
  private final TransferService transferService;

  @Transactional
  public void executeDue(UUID scheduleId) {
    ScheduledTransfer schedule = scheduledTransferRepository
      .findForUpdateById(scheduleId)
      .orElse(null);
    if (schedule == null || schedule.getStatus() != ScheduleStatus.ACTIVE) {
      return;
    }
    if (schedule.getNextRunAt().isAfter(Instant.now())) {
      // Already handled by an overlapping tick between the poll and this locked re-read.
      return;
    }

    boolean succeeded;
    String executionStatus;
    try {
      TransferResponse result = runTransfer(schedule);
      succeeded = result.status() == TransferStatus.COMPLETED;
      executionStatus = succeeded
        ? "COMPLETED"
        : "FAILED: " + (result.failureReason() != null ? result.failureReason() : result.status());
    } catch (RuntimeException exception) {
      log.warn("Scheduled transfer {} failed to execute", scheduleId, exception);
      succeeded = false;
      executionStatus = "FAILED: " + exception.getMessage();
    }

    schedule.setLastExecutedAt(Instant.now());
    schedule.setLastExecutionStatus(executionStatus);
    advance(schedule, succeeded);
    scheduledTransferRepository.save(schedule);
  }

  private TransferResponse runTransfer(ScheduledTransfer schedule) {
    CallerIdentity caller = new CallerIdentity(schedule.getOwnerUserId());
    // Ties the quote to this exact occurrence, so a crash-and-retry between quote and confirm
    // reuses the same pending transfer instead of double-executing it (NFR-R2).
    String idempotencyKey = "scheduled:" + schedule.getId() + ":" + schedule.getNextRunAt();
    TransferQuoteRequest request = new TransferQuoteRequest(
      schedule.getFromAccountId(),
      schedule.getToAccount(),
      schedule.getAmount(),
      schedule.getNote()
    );

    TransferResponse quoted = transferService.quote(caller, request, idempotencyKey);
    if (quoted.status() != TransferStatus.PENDING_CONFIRMATION) {
      return quoted;
    }
    return transferService.confirm(caller, quoted.id());
  }

  private void advance(ScheduledTransfer schedule, boolean succeeded) {
    if (schedule.getFrequency() == ScheduleFrequency.ONE_TIME) {
      schedule.setStatus(succeeded ? ScheduleStatus.COMPLETED : ScheduleStatus.FAILED);
      return;
    }

    Instant next = nextOccurrence(schedule);
    if (schedule.getEndDate() != null && next.isAfter(schedule.getEndDate())) {
      schedule.setStatus(ScheduleStatus.COMPLETED);
      return;
    }
    schedule.setNextRunAt(next);
  }

  private Instant nextOccurrence(ScheduledTransfer schedule) {
    return switch (schedule.getFrequency()) {
      case WEEKLY -> schedule.getNextRunAt().plus(Duration.ofDays(7));
      case MONTHLY -> schedule.getNextRunAt().atZone(SCHEDULE_ZONE).plusMonths(1).toInstant();
      case ONE_TIME -> schedule.getNextRunAt();
    };
  }
}
