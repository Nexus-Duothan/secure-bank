package com.securebank.transfer.service;

import com.securebank.transfer.config.TransferServiceProperties;
import com.securebank.transfer.dto.CreateScheduledTransferRequest;
import com.securebank.transfer.dto.ScheduledTransferResponse;
import com.securebank.transfer.dto.UpdateScheduleStatusRequest;
import com.securebank.transfer.entity.ScheduledTransfer;
import com.securebank.transfer.enums.ScheduleStatus;
import com.securebank.transfer.repository.ScheduledTransferRepository;
import com.securebank.transfer.security.CallerIdentity;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User-facing management of scheduled and recurring payments. Execution of due schedules
 * happens separately in {@link ScheduledTransferExecutionService}, driven by
 * {@link com.securebank.transfer.config.ScheduledTransferRunner}.
 */
@Service
@RequiredArgsConstructor
public class ScheduledTransferService {

  /** Statuses a customer is allowed to move a schedule to. */
  private static final Set<ScheduleStatus> USER_SETTABLE_STATUSES = Set.of(
    ScheduleStatus.ACTIVE,
    ScheduleStatus.PAUSED,
    ScheduleStatus.CANCELLED
  );

  /**
   * Statuses a schedule can still be moved out of. Cancelling is final: a cancelled
   * schedule must never be reactivated, or a payment the customer stopped could
   * silently start running again.
   */
  private static final Set<ScheduleStatus> CHANGEABLE_STATUSES = Set.of(
    ScheduleStatus.ACTIVE,
    ScheduleStatus.PAUSED
  );

  private final ScheduledTransferRepository scheduledTransferRepository;
  private final TransferServiceProperties properties;

  @Transactional
  public ScheduledTransferResponse create(
    CallerIdentity caller,
    CreateScheduledTransferRequest request
  ) {
    if (request.toAccount().equalsIgnoreCase(request.fromAccountId())) {
      throw new IllegalArgumentException("Cannot schedule a transfer to the same account");
    }
    if (request.amount().compareTo(properties.limits().perTransaction()) > 0) {
      throw new LimitExceededException(
        "Amount exceeds the per-transaction limit of " + properties.limits().perTransaction()
      );
    }
    if (request.startAt().isBefore(Instant.now())) {
      throw new IllegalArgumentException("startAt must be in the future");
    }
    if (request.endDate() != null && !request.endDate().isAfter(request.startAt())) {
      throw new IllegalArgumentException("endDate must be after startAt");
    }

    ScheduledTransfer saved = scheduledTransferRepository.save(
      ScheduledTransfer.builder()
        .ownerUserId(caller.userId())
        .fromAccountId(request.fromAccountId())
        .toAccount(request.toAccount())
        .amount(request.amount())
        .note(request.note())
        .frequency(request.frequency())
        .nextRunAt(request.startAt())
        .endDate(request.endDate())
        .status(ScheduleStatus.ACTIVE)
        .build()
    );
    return ScheduledTransferResponse.from(saved);
  }

  @Transactional(readOnly = true)
  public List<ScheduledTransferResponse> list(CallerIdentity caller) {
    return scheduledTransferRepository
      .findByOwnerUserIdOrderByCreatedAtDesc(caller.userId())
      .stream()
      .map(ScheduledTransferResponse::from)
      .toList();
  }

  @Transactional
  public ScheduledTransferResponse updateStatus(
    CallerIdentity caller,
    UUID scheduleId,
    UpdateScheduleStatusRequest request
  ) {
    if (!USER_SETTABLE_STATUSES.contains(request.status())) {
      throw new IllegalArgumentException("Status must be one of " + USER_SETTABLE_STATUSES);
    }

    ScheduledTransfer schedule = scheduledTransferRepository
      .findByIdAndOwnerUserId(scheduleId, caller.userId())
      .orElseThrow(() -> new EntityNotFoundException("Scheduled transfer not found"));

    if (!CHANGEABLE_STATUSES.contains(schedule.getStatus())) {
      throw new ConflictException(
        "Schedule is " + schedule.getStatus() + " and can no longer be changed"
      );
    }

    schedule.setStatus(request.status());
    return ScheduledTransferResponse.from(scheduledTransferRepository.save(schedule));
  }
}
