package com.example.idempotency.service;

import com.example.idempotency.dto.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Value("${app.kafka.order-topic}")
    private String orderTopic;

    public OrderEventProducer(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderPlacedEvent event) {
        kafkaTemplate.send(orderTopic, event.orderId(), event);
        log.info("Published OrderPlacedEvent eventId={} orderId={}", event.eventId(), event.orderId());
    }

    public void publishDuplicate(OrderPlacedEvent event, int deliveries) {
        for (int i = 0; i < deliveries; i++) {
            publish(event);
        }
    }
}
