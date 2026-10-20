package com.example.outbox.orderconsumer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        BigDecimal total,
        String description,
        Instant createdAt,
        String createdBy
) {
    public OrderCreatedEvent {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
    }
}
