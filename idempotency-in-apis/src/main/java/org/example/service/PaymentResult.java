package org.example.service;

import org.example.dto.PaymentResponse;

public record PaymentResult(PaymentResponse response, boolean created) {
}
