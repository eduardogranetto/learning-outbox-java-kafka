package com.example.outbox.orderapi.repository;

import com.example.outbox.orderapi.entity.OutboxRecord;
import com.example.outbox.orderapi.entity.OutboxStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface OutboxRepository extends ReactiveCrudRepository<OutboxRecord, UUID> {
    Flux<OutboxRecord> findByStatus(OutboxStatus status);
}
