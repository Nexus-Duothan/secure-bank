package com.securebank.transfer.messaging;

import com.securebank.transfer.entity.Transfer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Raises Spring application events for completed/failed transfers. {@link KafkaTransferEventListener}
 * relays them to Kafka only after the owning transaction commits, so a broker hiccup can never roll
 * back a transfer that already succeeded in Postgres.
 */
@Component
@RequiredArgsConstructor
public class TransferEventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  public void publishCompleted(Transfer transfer) {
    applicationEventPublisher.publishEvent(
      new TransferCompletedEvent(
        transfer.getId(),
        transfer.getInitiatedByUserId(),
        transfer.getFromAccountId(),
        transfer.getToAccount(),
        transfer.getAmount(),
        transfer.getCurrency(),
        Instant.now()
      )
    );
  }

  public void publishFailed(Transfer transfer, String reason) {
    applicationEventPublisher.publishEvent(
      new TransferFailedEvent(
        transfer.getId(),
        transfer.getInitiatedByUserId(),
        transfer.getFromAccountId(),
        transfer.getToAccount(),
        transfer.getAmount(),
        transfer.getCurrency(),
        reason,
        Instant.now()
      )
    );
  }
}
