package com.example.logistics.management.dto;

import com.example.logistics.management.entity.ShipmentStatus;
import java.time.Instant;
import java.util.UUID;

public record ShipmentEventResponse(
        UUID id, UUID shipmentId, String eventType, ShipmentStatus status,
        String details, Instant createdAt
) {
}
