package com.example.logistics.processing.outbox;

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

    @Column(name = "shipment_id", nullable = false)
    private UUID shipmentId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(nullable = false, length = 64)
    private String topic;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private @Nullable Instant publishedAt;

    protected OutboxMessageEntity() {
    }

    public OutboxMessageEntity(UUID id, UUID shipmentId, long sequenceNumber, String topic, String payload, Instant createdAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.sequenceNumber = sequenceNumber;
        this.topic = topic;
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

    public String getTopic() {
        return topic;
    }

    public @Nullable Instant getPublishedAt() {
        return publishedAt;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
