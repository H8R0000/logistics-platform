package com.example.logistics.courier.dto;

import com.example.logistics.courier.entity.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourierResponse(
        UUID id,
        String name,
        VehicleType vehicleType,
        BigDecimal maxWeight,
        BigDecimal currentLoad,
        boolean available,
        Instant updatedAt) {
}
