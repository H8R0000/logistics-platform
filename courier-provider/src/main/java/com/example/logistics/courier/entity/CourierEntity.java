package com.example.logistics.courier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "couriers")
public class CourierEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 32)
    private VehicleType vehicleType;

    @Column(name = "max_weight", nullable = false, precision = 15, scale = 3)
    private BigDecimal maxWeight;

    @Column(name = "current_load", nullable = false, precision = 15, scale = 3)
    private BigDecimal currentLoad;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CourierEntity() {
    }

    public void updateAvailability(boolean available, BigDecimal currentLoad, Instant updatedAt) {
        this.available = available;
        this.currentLoad = currentLoad;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public BigDecimal getMaxWeight() {
        return maxWeight;
    }

    public BigDecimal getCurrentLoad() {
        return currentLoad;
    }

    public boolean isAvailable() {
        return available;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
