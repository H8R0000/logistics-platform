package com.example.logistics.processing.kafka;

import com.example.logistics.processing.entity.ShipmentData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record ShipmentProcessingCommand(
        @Min(1) @Max(1) int schemaVersion,
        @NotNull UUID messageId,
        @NotNull UUID shipmentId,
        @NotNull UUID correlationId,
        @NotNull Instant createdAt,
        @NotBlank @Size(max = 255) String ownerId,
        @NotNull @Valid ShipmentData shipmentData) {
}
