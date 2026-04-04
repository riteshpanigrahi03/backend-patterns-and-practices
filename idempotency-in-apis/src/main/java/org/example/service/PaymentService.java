package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.PaymentRequest;
import org.example.dto.PaymentResponse;
import org.example.entity.IdempotencyRecord;
import org.example.entity.Payment;
import org.example.exception.IdempotencyConflictException;
import org.example.repository.IdempotencyRecordRepository;
import org.example.repository.PaymentRepository;
import org.example.util.PaymentReferenceGenerator;
import org.example.util.RequestHashUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String SUCCESS = "SUCCESS";

    private final PaymentRepository paymentRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Value("${app.payment.processing-delay-ms:3000}")
    private long processingDelayMs;

    @Transactional
    public PaymentResult createPayment(String idempotencyKey, PaymentRequest request) {
        String requestHash = RequestHashUtil.generateHash(request);

        return idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey)
                .map(record -> handleExistingRecord(record, requestHash))
                .orElseGet(() -> createNewPayment(idempotencyKey, requestHash, request));
    }

    private PaymentResult handleExistingRecord(IdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(
                    "Idempotency key already used with different request payload");
        }

        Payment payment = paymentRepository.findById(record.getPaymentId())
                .orElseThrow(() -> new IllegalStateException("Payment not found for existing idempotency record"));

        return new PaymentResult(mapToResponse(payment), false);
    }

    private PaymentResult createNewPayment(String idempotencyKey, String requestHash, PaymentRequest request) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .status(SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        payment = paymentRepository.save(payment);
        payment.setPaymentRef(PaymentReferenceGenerator.generate(payment.getId()));
        payment = paymentRepository.save(payment);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .paymentId(payment.getId())
                .createdAt(LocalDateTime.now())
                .build();
        idempotencyRecordRepository.save(record);

        return new PaymentResult(mapToResponse(payment), true);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentRef(payment.getPaymentRef())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();
    }
}
