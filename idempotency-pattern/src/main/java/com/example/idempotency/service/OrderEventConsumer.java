package com.example.idempotency.service;

import com.example.idempotency.dto.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final LoyaltyPointsService loyaltyPointsService;

    public OrderEventConsumer(LoyaltyPointsService loyaltyPointsService) {
        this.loyaltyPointsService = loyaltyPointsService;
    }

    @KafkaListener(topics = "${app.kafka.order-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent eventId={}, orderId={}, customerId={}",
                event.eventId(), event.orderId(), event.customerId());
        loyaltyPointsService.process(event);
    }
}
