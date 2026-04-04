package org.example.util;

public final class PaymentReferenceGenerator {

    private static final long BASE_SEQUENCE = 1000L;

    private PaymentReferenceGenerator() {
    }

    public static String generate(Long paymentId) {
        return "PAY-" + (BASE_SEQUENCE + paymentId);
    }
}
