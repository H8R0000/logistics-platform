package com.example.logistics.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment_events")
public class ShipmentEventEntity {

    @Id
    private UUID id;

    @Column(name = "shipment_id", nullable = false)
    private UUID shipmentId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ShipmentStatus status;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processing_sequence", nullable = false)
    private long processingSequence;

    protected ShipmentEventEntity() {
    }

    public ShipmentEventEntity(UUID id, UUID shipmentId, String eventType,
                               ShipmentStatus status, String details, Instant createdAt) {
        this(id, shipmentId, eventType, status, details, createdAt, 0);
    }

    public ShipmentEventEntity(UUID id, UUID shipmentId, String eventType,
                               ShipmentStatus status, String details, Instant createdAt, long processingSequence) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.eventType = eventType;
        this.status = status;
        this.details = details;
        this.createdAt = createdAt;
        this.processingSequence = processingSequence;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public String getEventType() {
        return eventType;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
