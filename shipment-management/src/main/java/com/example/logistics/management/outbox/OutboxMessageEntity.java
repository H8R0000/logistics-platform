package com.example.logistics.management.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessageEntity {

    @Id
    private UUID id;

    @Column(name = "shipment_id", nullable = false, unique = true)
    private UUID shipmentId;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private @Nullable Instant publishedAt;

    protected OutboxMessageEntity() {
    }

    public OutboxMessageEntity(UUID id, UUID shipmentId, String payload, Instant createdAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
