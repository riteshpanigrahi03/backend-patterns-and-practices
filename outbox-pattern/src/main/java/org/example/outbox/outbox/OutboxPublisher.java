package org.example.outbox.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC = "order-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:5000}")
    @Transactional
    public void publishNewEvents() {
        List<OutboxEvent> events = outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(TOPIC, String.valueOf(event.getAggregateId()), event.getPayload()).get();
                event.markProcessed();
                log.info("Outbox event published to Kafka: outboxId={}, aggregateId={}, topic={}", event.getId(), event.getAggregateId(), TOPIC);
            } catch (Exception e) {
                log.error("Failed to publish outbox event: outboxId={}", event.getId(), e);
            }
        }
    }
}
