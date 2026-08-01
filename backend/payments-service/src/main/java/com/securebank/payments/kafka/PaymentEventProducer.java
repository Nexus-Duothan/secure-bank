package com.securebank.payments.kafka;

import com.securebank.payments.kafka.event.PaymentCompletedEvent;
import com.securebank.payments.kafka.event.PaymentHeldEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * First Kafka producer in this repo — no consumer exists anywhere yet (including
 * notification-service, also unbuilt), so this is publish-only for now. Topic names are a
 * new convention (payments.<event>.v1), documented in README.md so a future consumer can
 * be built against this schema.
 */
@Component
public class PaymentEventProducer {

  private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String completedTopic;
  private final String heldTopic;

  public PaymentEventProducer(
    KafkaTemplate<String, Object> kafkaTemplate,
    @Value("${payments.kafka.completed-topic}") String completedTopic,
    @Value("${payments.kafka.held-topic}") String heldTopic
  ) {
    this.kafkaTemplate = kafkaTemplate;
    this.completedTopic = completedTopic;
    this.heldTopic = heldTopic;
  }

  public void publishCompleted(PaymentCompletedEvent event) {
    publish(completedTopic, event.getPaymentId().toString(), event);
  }

  public void publishHeld(PaymentHeldEvent event) {
    publish(heldTopic, event.getPaymentId().toString(), event);
  }

  private void publish(String topic, String key, Object event) {
    // kafkaTemplate.send() can itself throw synchronously (e.g. TimeoutException while
    // fetching topic metadata) before ever returning a future, on top of the future it
    // returns completing exceptionally — both paths must be swallowed. This is
    // publish-only groundwork with no consumer yet; a missing/unreachable broker must
    // never fail the payment request it's reporting on.
    try {
      kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
        if (ex != null) {
          log.warn("Failed to publish event to topic {}: {}", topic, ex.getMessage());
        }
      });
    } catch (Exception ex) {
      log.warn("Failed to publish event to topic {}: {}", topic, ex.getMessage());
    }
  }
}
