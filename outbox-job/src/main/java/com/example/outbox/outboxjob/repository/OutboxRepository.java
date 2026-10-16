package com.example.outbox.outboxjob.repository;

import com.example.outbox.outboxjob.entity.OutboxRecord;
import com.example.outbox.outboxjob.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxRecord, UUID> {
    List<OutboxRecord> findByStatus(OutboxStatus status);
}
