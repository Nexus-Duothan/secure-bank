package com.securebank.transfer.dto;

import com.securebank.transfer.entity.ScheduledTransfer;
import com.securebank.transfer.enums.ScheduleFrequency;
import com.securebank.transfer.enums.ScheduleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ScheduledTransferResponse(
  UUID id,
  String fromAccountId,
  String toAccount,
  BigDecimal amount,
  String note,
  ScheduleFrequency frequency,
  Instant nextRunAt,
  Instant endDate,
  ScheduleStatus status,
  Instant lastExecutedAt,
  String lastExecutionStatus,
  Instant createdAt
) {
  public static ScheduledTransferResponse from(ScheduledTransfer schedule) {
    return new ScheduledTransferResponse(
      schedule.getId(),
      schedule.getFromAccountId(),
      schedule.getToAccount(),
      schedule.getAmount(),
      schedule.getNote(),
      schedule.getFrequency(),
      schedule.getNextRunAt(),
      schedule.getEndDate(),
      schedule.getStatus(),
      schedule.getLastExecutedAt(),
      schedule.getLastExecutionStatus(),
      schedule.getCreatedAt()
    );
  }
}
