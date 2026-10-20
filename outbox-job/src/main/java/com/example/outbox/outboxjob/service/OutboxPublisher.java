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

    private final ReplicaOutboxRepository replicaOutboxRepository;
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
        List<OutboxRecord> pending = replicaOutboxRepository.findPending();
        for (OutboxRecord record : pending) {
            try {
                kafkaTemplate.send(TOPIC, record.getContent()).get();

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
