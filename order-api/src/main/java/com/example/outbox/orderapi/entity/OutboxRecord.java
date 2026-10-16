package com.example.outbox.orderapi.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.r2dbc.postgresql.codec.Json;
import java.time.Instant;
import java.util.UUID;

@Table("outbox")
public class OutboxRecord implements org.springframework.data.domain.Persistable<java.util.UUID> {
    @Id
    private UUID id;

    private OutboxStatus status;

    private Json content;

    @Column("created_at")
    private Instant createdAt;

    @Column("published_at")
    private Instant publishedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OutboxStatus getStatus() { return status; }
    public void setStatus(OutboxStatus status) { this.status = status; }
    public Json getContent() { return content; }
    public void setContent(Json content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    @org.springframework.data.annotation.Transient
    private boolean isNew = true;
    @Override
    public boolean isNew() { return isNew; }
}
