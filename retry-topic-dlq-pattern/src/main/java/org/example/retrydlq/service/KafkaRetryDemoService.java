package org.example.retrydlq.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.example.retrydlq.domain.ProcessedOrder;
import org.example.retrydlq.dto.CreateOrderRequest;
import org.example.retrydlq.dto.EventPublishResponse;
import org.example.retrydlq.dto.OrderPlacedEvent;
import org.example.retrydlq.repository.ProcessedOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class KafkaRetryDemoService {

    private static final Logger log = LoggerFactory.getLogger(KafkaRetryDemoService.class);
    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final ProcessedOrderRepository processedOrderRepository;
    private final int maxAttempts;
    private final boolean failEnabled;
    private final String mainTopic;
    private final String retryTopic;
    private final String dlqTopic;

    public KafkaRetryDemoService(
            KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate,
            ProcessedOrderRepository processedOrderRepository,
            @Value("${app.retry.max-attempts}") int maxAttempts,
            @Value("${app.processing.fail-enabled}") boolean failEnabled,
            @Value("${app.topics.main}") String mainTopic,
            @Value("${app.topics.retry}") String retryTopic,
            @Value("${app.topics.dlq}") String dlqTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.processedOrderRepository = processedOrderRepository;
        this.maxAttempts = maxAttempts;
        this.failEnabled = failEnabled;
        this.mainTopic = mainTopic;
        this.retryTopic = retryTopic;
        this.dlqTopic = dlqTopic;
    }

    public EventPublishResponse publish(CreateOrderRequest request) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                request.orderId(),
                request.customerId(),
                request.amount()
        );
        sendToMainTopic(event);
        return new EventPublishResponse(
                "Order event published to Kafka for asynchronous processing",
                event.eventId(),
                mainTopic
        );
    }

    @KafkaListener(topics = "${app.topics.main}", groupId = "order-events-main-consumer")
    public void consumeMainTopic(OrderPlacedEvent event) {
        log.info("Received event on main topic. eventId={}, orderId={}", event.eventId(), event.orderId());

        try {
            process(event);
        } catch (RuntimeException ex) {
            log.warn("Failure occurred on main topic for eventId={}. Sending to retry topic. reason={}",
                    event.eventId(), ex.getMessage());
            sendToRetryTopic(event, 1);
        }
    }

    @KafkaListener(topics = "${app.topics.retry}", groupId = "order-events-retry-consumer")
    public void consumeRetryTopic(ConsumerRecord<String, OrderPlacedEvent> record) {
        OrderPlacedEvent event = record.value();
        int retryCount = readRetryCount(record);

        log.info("Received event on retry topic. eventId={}, orderId={}, retryCount={}",
                event.eventId(), event.orderId(), retryCount);

        try {
            process(event);
        } catch (RuntimeException ex) {
            log.warn("Failure occurred on retry topic for eventId={}. retryCount={}, reason={}",
                    event.eventId(), retryCount, ex.getMessage());

            if (retryCount >= maxAttempts) {
                log.warn("Max retries reached for eventId={}. Sending to DLQ.", event.eventId());
                sendToDlq(event, retryCount);
                return;
            }

            int nextRetryCount = retryCount + 1;
            log.info("Retry attempt count increased for eventId={} to {}", event.eventId(), nextRetryCount);
            sendToRetryTopic(event, nextRetryCount);
        }
    }

    @KafkaListener(topics = "${app.topics.dlq}", groupId = "order-events-dlq-consumer")
    public void consumeDlqTopic(OrderPlacedEvent event) {
        log.error("Received event in DLQ. eventId={}, orderId={}", event.eventId(), event.orderId());
    }

    void process(OrderPlacedEvent event) {
        log.info("Processing eventId={}, orderId={}", event.eventId(), event.orderId());

        if (failEnabled) {
            throw new RuntimeException("Demo failure is enabled by app.processing.fail-enabled=true");
        }

        if (processedOrderRepository.existsByEventId(event.eventId())) {
            log.info("Order already processed for eventId={}. Skipping duplicate success path.", event.eventId());
            return;
        }

        ProcessedOrder processedOrder = new ProcessedOrder(
                event.eventId(),
                event.orderId(),
                event.customerId(),
                event.amount(),
                LocalDateTime.now()
        );
        processedOrderRepository.save(processedOrder);
        log.info("Successfully processed eventId={}, orderId={} and saved it to H2",
                event.eventId(), event.orderId());
    }

    private void sendToMainTopic(OrderPlacedEvent event) {
        log.info("Publishing eventId={} to main topic={}", event.eventId(), mainTopic);
        kafkaTemplate.send(buildMessage(mainTopic, event, 0));
    }

    private void sendToRetryTopic(OrderPlacedEvent event, int retryCount) {
        log.info("Sending eventId={} to retry topic={} with retryCount={}",
                event.eventId(), retryTopic, retryCount);
        kafkaTemplate.send(buildMessage(retryTopic, event, retryCount));
    }

    private void sendToDlq(OrderPlacedEvent event, int retryCount) {
        log.info("Sending eventId={} to DLQ topic={} after retryCount={}",
                event.eventId(), dlqTopic, retryCount);
        kafkaTemplate.send(buildMessage(dlqTopic, event, retryCount));
    }

    private Message<OrderPlacedEvent> buildMessage(String topic, OrderPlacedEvent event, int retryCount) {
        return MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, event.eventId())
                .setHeader(RETRY_COUNT_HEADER, String.valueOf(retryCount))
                .build();
    }

    private int readRetryCount(ConsumerRecord<String, OrderPlacedEvent> record) {
        Header header = record.headers().lastHeader(RETRY_COUNT_HEADER);
        if (header == null || header.value() == null || header.value().length == 0) {
            return 0;
        }

        return Integer.parseInt(new String(header.value(), StandardCharsets.UTF_8));
    }
}
