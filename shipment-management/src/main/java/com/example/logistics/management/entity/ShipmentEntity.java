package com.example.logistics.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "shipments")
public class ShipmentEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "source_id", length = 64)
    private @Nullable String sourceId;

    @Column(name = "source_message_id")
    private @Nullable UUID sourceMessageId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Address origin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Address destination;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal weight;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Dimensions dimensions;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 32)
    private DeliveryType deliveryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ShipmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /** Номер последнего применённого события B; защищает от дублей и запоздалых событий. */
    @Column(name = "last_processing_sequence", nullable = false)
    private long lastProcessingSequence;

    @Version
    @Column(nullable = false)
    private Long version;

    protected ShipmentEntity() {
    }

    public ShipmentEntity(UUID id, String ownerId, Address origin,
                          Address destination, BigDecimal weight,
                          Dimensions dimensions, DeliveryType deliveryType, Instant createdAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.origin = origin;
        this.destination = destination;
        this.weight = weight;
        this.dimensions = dimensions;
        this.deliveryType = deliveryType;
        this.status = ShipmentStatus.CREATED;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Address getOrigin() {
        return origin;
    }

    public Address getDestination() {
        return destination;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public long getLastProcessingSequence() {
        return lastProcessingSequence;
    }

    /** Вызывается после проверки контракта и порядка событий под блокировкой строки заявки. */
    public void applyProcessingResult(ShipmentStatus status, long sequenceNumber,
            @Nullable Instant deliveredAt, Instant now) {
        this.status = status;
        this.lastProcessingSequence = sequenceNumber;
        this.deliveredAt = deliveredAt;
        this.updatedAt = now;
    }
}
