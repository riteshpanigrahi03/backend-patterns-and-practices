package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.ErrorResponse;
import org.example.dto.PaymentRequest;
import org.example.dto.PaymentResponse;
import org.example.exception.IdempotencyConflictException;
import org.example.service.PaymentResult;
import org.example.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody PaymentRequest request) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return buildError(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }

        if (!isValidRequest(request)) {
            return buildError(HttpStatus.BAD_REQUEST, "orderId and amount are required");
        }

        try {
            PaymentResult result = paymentService.createPayment(idempotencyKey, request);
            HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(result.response());
        } catch (IdempotencyConflictException ex) {
            return buildError(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private boolean isValidRequest(PaymentRequest request) {
        return request != null
                && StringUtils.hasText(request.getOrderId())
                && request.getAmount() != null
                && request.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
