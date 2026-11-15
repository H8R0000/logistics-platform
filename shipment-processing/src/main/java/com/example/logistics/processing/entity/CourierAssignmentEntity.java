package com.example.logistics.processing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Назначение курьера на заявку, которым управляет B. Состояние курьера в C при этом не меняется.
 * Уникальные индексы БД разрешают только одно ACTIVE-назначение на курьера и на заявку.
 */
@Entity
@Table(name = "courier_assignments")
public class CourierAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "shipment_id", nullable = false)
    private UUID shipmentId;

    @Column(name = "courier_id", nullable = false)
    private UUID courierId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssignmentStatus status;

    protected CourierAssignmentEntity() {
    }

    public UUID getId() { return id; }
    public UUID getShipmentId() { return shipmentId; }
    public UUID getCourierId() { return courierId; }
    public Instant getAssignedAt() { return assignedAt; }
    public AssignmentStatus getStatus() { return status; }
}
