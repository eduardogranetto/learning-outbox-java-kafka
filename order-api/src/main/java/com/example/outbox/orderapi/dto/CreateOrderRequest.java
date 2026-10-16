package com.example.outbox.orderapi.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotNull BigDecimal total,
        String description,
        String createdBy
) {}
