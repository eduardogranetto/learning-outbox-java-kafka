package com.example.outbox.orderapi.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("orders")
public class Order implements org.springframework.data.domain.Persistable<java.util.UUID> {
    @Id
    private UUID id;

    @Column("total")
    private BigDecimal total;

    private String description;

    @Column("created_at")
    private Instant createdAt;

    @Column("created_by")
    private String createdBy;

    @Column("status")
    private OrderStatus status = OrderStatus.PENDING;

    @Column("processed_at")
    private Instant processedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    @org.springframework.data.annotation.Transient
    private boolean isNew = true;
    @Override
    public boolean isNew() { return isNew; }

    public enum OrderStatus {
        PENDING,
        PROCESSED
    }
}
