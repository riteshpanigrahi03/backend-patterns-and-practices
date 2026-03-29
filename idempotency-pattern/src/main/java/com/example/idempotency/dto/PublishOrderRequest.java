package com.example.idempotency.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PublishOrderRequest(
        String eventId,
        @NotBlank String orderId,
        @NotBlank String customerId,
        @NotNull @DecimalMin("0.0") BigDecimal orderTotal,
        @Min(1) int rewardPoints
) {
}
