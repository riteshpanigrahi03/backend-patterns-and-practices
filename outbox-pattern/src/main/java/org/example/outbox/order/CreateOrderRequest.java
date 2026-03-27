package org.example.outbox.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String productName,
        @Min(1) Integer quantity
) {
}
