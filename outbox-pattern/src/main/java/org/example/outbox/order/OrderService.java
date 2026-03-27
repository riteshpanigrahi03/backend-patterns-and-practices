package org.example.outbox.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.outbox.outbox.OutboxEvent;
import org.example.outbox.outbox.OutboxEventRepository;
import org.example.outbox.outbox.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderRepository orderRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(new Order(request.productName(), request.quantity()));
        log.info("Order created: id={}, productName={}, quantity={}", order.getId(), order.getProductName(), order.getQuantity());

        String payload = toJson(order);
        OutboxEvent outboxEvent = new OutboxEvent(order.getId(), "ORDER_CREATED", payload, OutboxStatus.NEW);
        outboxEventRepository.save(outboxEvent);
        log.info("Outbox event inserted: id={}, aggregateId={}, eventType={}", outboxEvent.getId(), outboxEvent.getAggregateId(), outboxEvent.getEventType());

        return order;
    }

    private String toJson(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order to JSON", e);
        }
    }
}
