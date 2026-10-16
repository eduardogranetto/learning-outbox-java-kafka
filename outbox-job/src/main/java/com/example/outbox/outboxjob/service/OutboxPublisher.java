package com.example.outbox.outboxjob.service;

import com.example.outbox.outboxjob.entity.OutboxRecord;
import com.example.outbox.outboxjob.entity.OutboxStatus;
import com.example.outbox.outboxjob.repository.OutboxRepository;
import com.example.outbox.outboxjob.repository.ReplicaOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC = "order_created";

    // Reads pending records from the PostgreSQL replica (read-only).
    private final ReplicaOutboxRepository replicaOutboxRepository;
    // Writes status updates to the primary PostgreSQL instance (read-write).
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(ReplicaOutboxRepository replicaOutboxRepository,
                           OutboxRepository outboxRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.replicaOutboxRepository = replicaOutboxRepository;
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${outbox.poll.ms:1000}")
    @Transactional
    public void publishPending() {
        // Read from replica. Records are detached (not managed by the primary JPA repository).
        List<OutboxRecord> pending = replicaOutboxRepository.findPending();
        for (OutboxRecord record : pending) {
            try {
                kafkaTemplate.send(TOPIC, record.getContent()).get();

                // Persist the status change on the primary instance. Re-load the record from the
                // primary so we operate on a managed entity; this also guards against publishing a
                // record twice if the replica is lagging behind and still reports it as PENDING.
                OutboxRecord managed = outboxRepository.findById(record.getId())
                        .orElseThrow(() -> new NoSuchElementException(
                                "Outbox record not found on primary: " + record.getId()));
                if (managed.getStatus() == OutboxStatus.PUBLISHED) {
                    log.info("Outbox record {} already published on primary, skipping", record.getId());
                    continue;
                }
                managed.setStatus(OutboxStatus.PUBLISHED);
                managed.setPublishedAt(Instant.now());
                outboxRepository.save(managed);
                log.info("Published outbox record {} to {}", record.getId(), TOPIC);
            } catch (Exception e) {
                log.error("Failed to publish outbox record {}", record.getId(), e);
            }
        }
    }
}
