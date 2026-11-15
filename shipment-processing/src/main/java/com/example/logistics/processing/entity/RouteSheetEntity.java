package com.example.logistics.processing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Маршрутный лист одной заявки. Время доставки сохраняется один раз и переживает перезапуск B. */
@Entity
@Table(name = "route_sheets")
public class RouteSheetEntity {

    @Id
    private UUID id;
    @Column(name = "shipment_id", nullable = false, unique = true)
    private UUID shipmentId;
    @Column(name = "courier_id", nullable = false)
    private UUID courierId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Address origin;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Address destination;
    @Column(name = "estimated_delivery_hours", nullable = false, precision = 15, scale = 9)
    private BigDecimal estimatedDeliveryHours;
    @Column(name = "estimated_delivery_at", nullable = false)
    private Instant estimatedDeliveryAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RouteSheetStatus status;

    protected RouteSheetEntity() {
    }

    public RouteSheetEntity(UUID id, UUID shipmentId, UUID courierId, Address origin,
                            Address destination, Duration duration, Instant createdAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.courierId = courierId;
        this.origin = origin;
        this.destination = destination;
        this.estimatedDeliveryHours = BigDecimal.valueOf(duration.toMillis())
                .divide(BigDecimal.valueOf(3_600_000), 9, RoundingMode.HALF_UP);
        this.estimatedDeliveryAt = createdAt.plus(duration);
        this.createdAt = createdAt;
        this.status = RouteSheetStatus.IN_TRANSIT;
    }

    public UUID getId() { return id; }
    public UUID getShipmentId() { return shipmentId; }
    public UUID getCourierId() { return courierId; }
    public Address getOrigin() { return origin; }
    public Address getDestination() { return destination; }
    public BigDecimal getEstimatedDeliveryHours() { return estimatedDeliveryHours; }
    public Instant getEstimatedDeliveryAt() { return estimatedDeliveryAt; }
    public Instant getCreatedAt() { return createdAt; }
    public RouteSheetStatus getStatus() { return status; }

    public void markDelivered() {
        status = RouteSheetStatus.DELIVERED;
    }
}
