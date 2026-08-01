package com.securebank.transfer.dto;

import com.securebank.transfer.entity.Payee;
import java.time.Instant;
import java.util.UUID;

public record PayeeResponse(
  UUID id,
  String nickname,
  String accountReference,
  Instant coolingOffUntil,
  boolean coolingOff,
  Instant createdAt
) {
  public static PayeeResponse from(Payee payee) {
    Instant now = Instant.now();
    return new PayeeResponse(
      payee.getId(),
      payee.getNickname(),
      payee.getAccountReference(),
      payee.getCoolingOffUntil(),
      payee.isCoolingOff(now),
      payee.getCreatedAt()
    );
  }
}
