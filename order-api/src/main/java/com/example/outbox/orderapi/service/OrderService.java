package com.example.outbox.orderapi.service;

import com.example.outbox.orderapi.dto.CreateOrderRequest;
import com.example.outbox.orderapi.dto.OrderCreatedEvent;
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
                        OrderCreatedEvent event = new OrderCreatedEvent(
                                UUID.randomUUID(),
                                "order_created",
                                saved.getId(),
                                saved.getTotal(),
                                saved.getDescription(),
                                saved.getCreatedAt(),
                                saved.getCreatedBy()
                        );
                        OutboxRecord outbox = new OutboxRecord();
                        outbox.setId(UUID.randomUUID());
                        outbox.setStatus(OutboxStatus.PENDING);
                        outbox.setContent(Json.of(objectMapper.writeValueAsString(event)));
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
