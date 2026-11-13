package com.example.logistics.management.dto;

import com.example.logistics.management.entity.DeliveryType;
import com.example.logistics.management.entity.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String ownerId,
        AddressDto origin,
        AddressDto destination,
        BigDecimal weight,
        DimensionsDto dimensions,
        DeliveryType deliveryType,
        ShipmentStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant deliveredAt
) {
}
