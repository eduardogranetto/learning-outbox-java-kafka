package com.example.outbox.orderconsumer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);

    private final JdbcTemplate jdbcTemplate;

    public OrderStatusService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void markProcessed(UUID orderId) {
        int updated = jdbcTemplate.update(
                "UPDATE orders SET status = 'PROCESSED', processed_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()), orderId);
        log.info("Marked order {} as PROCESSED (rows updated: {})", orderId, updated);
    }
}
