package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponse {

    private final String paymentRef;
    private final String orderId;
    private final BigDecimal amount;
    private final String status;
}
