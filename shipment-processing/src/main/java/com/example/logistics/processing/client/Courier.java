package com.example.logistics.processing.client;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Courier(@NotNull UUID id, @NotBlank String name, @NotNull VehicleType vehicleType,
                      @NotNull @DecimalMin("0.001") BigDecimal maxWeight,
                      @NotNull @DecimalMin("0") BigDecimal currentLoad,
                      boolean available, @NotNull Instant updatedAt) {

    public BigDecimal remainingCapacity() {
        return maxWeight.subtract(currentLoad);
    }

    public enum VehicleType {
        BIKE, CAR, VAN
    }
}
