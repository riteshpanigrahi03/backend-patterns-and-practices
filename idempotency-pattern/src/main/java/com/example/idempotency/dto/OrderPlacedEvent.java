package com.example.idempotency.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderPlacedEvent(
        String eventId,
        String orderId,
        String customerId,
        BigDecimal orderTotal,
        int rewardPoints,
        Instant placedAt
) {
}
