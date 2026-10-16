package com.example.outbox.orderapi.service;

import com.example.outbox.orderapi.dto.CreateOrderRequest;
import com.example.outbox.orderapi.dto.OrderResponse;
import com.example.outbox.orderapi.entity.Order;
import com.example.outbox.orderapi.entity.OutboxRecord;
import com.example.outbox.orderapi.entity.OutboxStatus;
import com.example.outbox.orderapi.repository.OrderRepository;
import com.example.outbox.orderapi.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public Mono<OrderResponse> createOrder(CreateOrderRequest req) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setTotal(req.total());
        order.setDescription(req.description());
        order.setCreatedBy(req.createdBy());
        order.setCreatedAt(Instant.now());

        return orderRepository.save(order)
                .flatMap(saved -> {
                    try {
                        Map<String, Object> content = new LinkedHashMap<>();
                        content.put("eventId", UUID.randomUUID().toString());
                        content.put("eventType", "order_created");
                        content.put("orderId", saved.getId());
                        content.put("total", saved.getTotal());
                        content.put("description", saved.getDescription());
                        content.put("createdAt", saved.getCreatedAt());
                        content.put("createdBy", saved.getCreatedBy());
                        String json = objectMapper.writeValueAsString(content);

                        OutboxRecord outbox = new OutboxRecord();
                        outbox.setId(UUID.randomUUID());
                        outbox.setStatus(OutboxStatus.PENDING);
                        outbox.setContent(Json.of(json));
                        outbox.setCreatedAt(Instant.now());
                        return outboxRepository.save(outbox).thenReturn(saved);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .map(saved -> new OrderResponse(saved.getId(), saved.getTotal(), saved.getDescription(), saved.getCreatedAt(), saved.getCreatedBy(), saved.getStatus(), saved.getProcessedAt()));
    }

    public Mono<OrderResponse> getOrderById(UUID id) {
        return orderRepository.findById(id)
                .map(order -> new OrderResponse(order.getId(), order.getTotal(), order.getDescription(), order.getCreatedAt(), order.getCreatedBy(), order.getStatus(), order.getProcessedAt()));
    }

    /**
     * SSE stream of the status of a specific order.
     * Polls the database every 2 seconds and emits an {@code order-status} event whenever the status
     * (or processedAt) changes. Emits the current value immediately on subscription.
     */
    public Flux<ServerSentEvent<OrderStatusDto>> statusStream(UUID id) {
        return Flux.interval(Duration.ofSeconds(2))
                .startWith(0L)
                .flatMap(tick -> orderRepository.findById(id)
                        .map(order -> new OrderStatusDto(order.getStatus(), order.getProcessedAt()))
                        .defaultIfEmpty(new OrderStatusDto(null, null)))
                .distinctUntilChanged()
                .map(dto -> ServerSentEvent.builder(dto)
                        .event("order-status")
                        .build());
    }

    public record OrderStatusDto(Order.OrderStatus status, Instant processedAt) {}
}
