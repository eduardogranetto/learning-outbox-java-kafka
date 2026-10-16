package com.example.outbox.orderconsumer.consumer;

import com.example.outbox.orderconsumer.service.OrderStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final OrderStatusService orderStatusService;
    private final ObjectMapper objectMapper;

    public OrderCreatedConsumer(OrderStatusService orderStatusService, ObjectMapper objectMapper) {
        this.orderStatusService = orderStatusService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order_created", groupId = "order-consumer-group")
    public void consume(String message) {
        log.info("Received order_created event: {}", message);
        try {
            Map<String, Object> content = objectMapper.readValue(message, Map.class);
            Object orderIdObj = content.get("orderId");
            if (orderIdObj == null) {
                log.warn("Consumed message has no orderId field; skipping status update");
                return;
            }
            UUID orderId = UUID.fromString(orderIdObj.toString());
            orderStatusService.markProcessed(orderId);
        } catch (Exception e) {
            log.error("Failed to process order_created event: {}", message, e);
        }
    }
}
