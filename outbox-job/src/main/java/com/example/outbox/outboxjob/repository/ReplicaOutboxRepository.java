package com.example.outbox.outboxjob.repository;

import com.example.outbox.outboxjob.entity.OutboxRecord;
import com.example.outbox.outboxjob.entity.OutboxStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Read-only repository that fetches pending outbox records from the PostgreSQL <b>replica</b>.
 *
 * Entities returned here are detached (they are not managed by the primary JPA
 * {@link OutboxRepository}), which is intentional: reads must not happen against the primary so the
 * replica can absorb the polling load.
 */
@Repository
public class ReplicaOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReplicaOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<OutboxRecord> findPending() {
        return jdbcTemplate.query(
                """
                    SELECT id, status, content, created_at, published_at
                    FROM outbox
                    WHERE status = 'PENDING'
                    ORDER BY created_at ASC
                """,
                outboxRowMapper());
    }

    private RowMapper<OutboxRecord> outboxRowMapper() {
        return (rs, rowNum) -> {
            OutboxRecord record = new OutboxRecord();
            record.setId(UUID.fromString(rs.getString("id")));
            record.setStatus(OutboxStatus.valueOf(rs.getString("status")));
            record.setContent(rs.getString("content"));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                record.setCreatedAt(createdAt.toInstant());
            }

            Timestamp publishedAt = rs.getTimestamp("published_at");
            if (publishedAt != null) {
                record.setPublishedAt(publishedAt.toInstant());
            }

            return record;
        };
    }
}
