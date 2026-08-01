package com.securebank.lending.kafka;

import com.securebank.lending.kafka.event.LoanDisbursedEvent;
import com.securebank.lending.kafka.event.RepaymentOverdueEvent;
import com.securebank.lending.kafka.event.RepaymentReminderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publish-only for now — no consumer exists for these topics yet (notification-service's
 * NotificationEventConsumer doesn't subscribe to them as of this writing). Topic names follow
 * payments-service's convention (loans.<event>.v1) so a future consumer can be built against
 * this schema. Uses raw JSON (JsonSerializer) rather than app-internal event relaying, since
 * the eventual consumer is a different microservice/codebase.
 */
@Component
public class LoanEventProducer {

  private static final Logger log = LoggerFactory.getLogger(LoanEventProducer.class);

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String disbursedTopic;
  private final String reminderTopic;
  private final String overdueTopic;

  public LoanEventProducer(
    KafkaTemplate<String, Object> kafkaTemplate,
    @Value("${lending.kafka.disbursed-topic}") String disbursedTopic,
    @Value("${lending.kafka.reminder-topic}") String reminderTopic,
    @Value("${lending.kafka.overdue-topic}") String overdueTopic
  ) {
    this.kafkaTemplate = kafkaTemplate;
    this.disbursedTopic = disbursedTopic;
    this.reminderTopic = reminderTopic;
    this.overdueTopic = overdueTopic;
  }

  public void publishDisbursed(LoanDisbursedEvent event) {
    publish(disbursedTopic, event.loanId().toString(), event);
  }

  public void publishReminder(RepaymentReminderEvent event) {
    publish(reminderTopic, event.loanId().toString(), event);
  }

  public void publishOverdue(RepaymentOverdueEvent event) {
    publish(overdueTopic, event.loanId().toString(), event);
  }

  private void publish(String topic, String key, Object event) {
    // kafkaTemplate.send() can itself throw synchronously (e.g. while fetching topic
    // metadata) before ever returning a future, on top of the future completing
    // exceptionally — both paths must be swallowed. A missing/unreachable broker must never
    // fail the request it's reporting on (see max.block.ms in application.yml).
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
