package com.example.outbox.orderapi.dto;

import com.example.outbox.orderapi.entity.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        BigDecimal total,
        String description,
        Instant createdAt,
        String createdBy,
        Order.OrderStatus status,
        Instant processedAt
) {}
