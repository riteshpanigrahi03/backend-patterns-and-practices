package org.example.retrydlq.dto;

public record EventPublishResponse(
        String message,
        String eventId,
        String topic
) {
}
