package com.securebank.transfer.dto;

import com.securebank.transfer.entity.Transfer;
import com.securebank.transfer.enums.TransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
  UUID id,
  TransferStatus status,
  String fromAccountId,
  String toAccount,
  BigDecimal amount,
  BigDecimal fee,
  BigDecimal totalDebit,
  String currency,
  String note,
  String failureReason,
  Instant createdAt,
  Instant confirmedAt
) {
  public static TransferResponse from(Transfer transfer) {
    return new TransferResponse(
      transfer.getId(),
      transfer.getStatus(),
      transfer.getFromAccountId(),
      transfer.getToAccount(),
      transfer.getAmount(),
      transfer.getFee(),
      transfer.totalDebit(),
      transfer.getCurrency(),
      transfer.getNote(),
      transfer.getFailureReason(),
      transfer.getCreatedAt(),
      transfer.getConfirmedAt()
    );
  }
}
