package org.example.retrydlq.dto;

import java.math.BigDecimal;

public record OrderPlacedEvent(
        String eventId,
        String orderId,
        String customerId,
        BigDecimal amount
) {
}
