package com.example.logistics.management.kafka;

import com.example.logistics.management.dto.CreateShipmentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Повтор операции сохраняет sourceId и messageId; владельца определяет A. */
public record ExternalShipmentRequest(
        @Min(1) @Max(1) int schemaVersion,
        @NotNull UUID messageId,
        @NotBlank @Size(max = 64) String sourceId,
        @NotNull @Valid CreateShipmentRequest shipmentData) {
}
