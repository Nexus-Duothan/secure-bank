package com.securebank.transfer.messaging;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class KafkaTransferEventListener {

  private static final String TOPIC = "transfer-events";
  private static final Logger log = LoggerFactory.getLogger(KafkaTransferEventListener.class);

  private final KafkaTemplate<Object, Object> kafkaTemplate;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTransferCompleted(TransferCompletedEvent event) {
    send(event.transferId().toString(), event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onTransferFailed(TransferFailedEvent event) {
    send(event.transferId().toString(), event);
  }

  private void send(String key, Object event) {
    kafkaTemplate.send(TOPIC, key, event).whenComplete((result, exception) -> {
      if (exception != null) {
        log.warn("Failed to publish {} to {}", event.getClass().getSimpleName(), TOPIC, exception);
      }
    });
  }
}
