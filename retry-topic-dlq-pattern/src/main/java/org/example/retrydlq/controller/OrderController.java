package org.example.retrydlq.controller;

import jakarta.validation.Valid;
import org.example.retrydlq.dto.CreateOrderRequest;
import org.example.retrydlq.dto.EventPublishResponse;
import org.example.retrydlq.service.KafkaRetryDemoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class OrderController {

    private final KafkaRetryDemoService kafkaRetryDemoService;

    public OrderController(KafkaRetryDemoService kafkaRetryDemoService) {
        this.kafkaRetryDemoService = kafkaRetryDemoService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EventPublishResponse publishOrderEvent(@Valid @RequestBody CreateOrderRequest request) {
        return kafkaRetryDemoService.publish(request);
    }
}
