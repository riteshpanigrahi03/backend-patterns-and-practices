package com.example.idempotency.controller;

import com.example.idempotency.dto.OrderPlacedEvent;
import com.example.idempotency.dto.PublishOrderRequest;
import com.example.idempotency.service.OrderEventProducer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderEventProducer orderEventProducer;

    public OrderController(OrderEventProducer orderEventProducer) {
        this.orderEventProducer = orderEventProducer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderPlacedEvent publishOrder(@Valid @RequestBody PublishOrderRequest request) {
        OrderPlacedEvent event = toEvent(request, request.eventId());
        orderEventProducer.publish(event);
        return event;
    }

    @PostMapping("/duplicate")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderPlacedEvent publishDuplicateOrder(
            @Valid @RequestBody PublishOrderRequest request,
            @RequestParam(defaultValue = "2") int deliveries) {
        OrderPlacedEvent event = toEvent(request, request.eventId());
        orderEventProducer.publishDuplicate(event, deliveries);
        return event;
    }

    @PostMapping("/{eventId}/redeliver")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderPlacedEvent reDeliverOrder(
            @PathVariable String eventId,
            @Valid @RequestBody PublishOrderRequest request,
            @RequestParam(defaultValue = "1") int deliveries) {
        OrderPlacedEvent event = toEvent(request, eventId);
        orderEventProducer.publishDuplicate(event, deliveries);
        return event;
    }

    private OrderPlacedEvent toEvent(PublishOrderRequest request, String explicitEventId) {
        String eventId = explicitEventId == null || explicitEventId.isBlank()
                ? UUID.randomUUID().toString()
                : explicitEventId;
        return new OrderPlacedEvent(
                eventId,
                request.orderId(),
                request.customerId(),
                request.orderTotal(),
                request.rewardPoints(),
                Instant.now());
    }
}
