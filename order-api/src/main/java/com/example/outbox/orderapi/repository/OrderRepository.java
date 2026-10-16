package com.example.outbox.orderapi.repository;

import com.example.outbox.orderapi.entity.Order;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import java.util.UUID;

public interface OrderRepository extends ReactiveCrudRepository<Order, UUID> {}
