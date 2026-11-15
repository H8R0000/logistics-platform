package com.example.logistics.management.kafka;

import com.example.logistics.management.entity.ShipmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Входящий контракт B → A; sequenceNumber возрастает отдельно для каждой заявки. */
public record ShipmentProcessingEvent(
        @Min(1) @Max(1) int schemaVersion,
        @NotNull UUID eventId,
        @NotNull UUID shipmentId,
        @NotBlank @Size(max = 64) String eventType,
        @NotNull Instant timestamp,
        @NotNull @Valid Payload payload,
        @NotNull UUID correlationId,
        @Min(1) long sequenceNumber) {

    /** Проверяется после Bean Validation: тип события, статус и этап должны соответствовать друг другу. */
    public boolean hasConsistentResult() {
        ShipmentStatus status = payload.status();
        String stage = payload.currentStage();
        boolean consistent = switch (eventType) {
            case "SHIPMENT_PROCESSING_STARTED", "COURIER_SEARCH_RETRY_SCHEDULED" ->
                    status == ShipmentStatus.PROCESSING && "SEARCH_COURIER".equals(stage);
            case "SHIPMENT_WAITING_FOR_COURIER" ->
                    status == ShipmentStatus.WAITING_FOR_COURIER && "SEARCH_COURIER".equals(stage);
            case "COURIER_NOT_FOUND" ->
                    status == ShipmentStatus.COURIER_NOT_FOUND && "SEARCH_COURIER".equals(stage);
            case "COURIER_ASSIGNED", "ROUTE_SHEET_CREATED" ->
                    status == ShipmentStatus.PROCESSING && "CREATE_ROUTE".equals(stage);
            case "SHIPMENT_IN_TRANSIT" ->
                    status == ShipmentStatus.IN_TRANSIT && "CONFIRM_DELIVERY".equals(stage);
            case "SHIPMENT_DELIVERED" ->
                    status == ShipmentStatus.DELIVERED && "CONFIRM_DELIVERY".equals(stage);
            case "SHIPMENT_FAILED" -> status == ShipmentStatus.FAILED;
            case "SHIPMENT_PROCESSING_RESUMED" ->
                    (status == ShipmentStatus.PROCESSING
                            && ("SEARCH_COURIER".equals(stage) || "CREATE_ROUTE".equals(stage)))
                    || (status == ShipmentStatus.IN_TRANSIT && "CONFIRM_DELIVERY".equals(stage));
            case "DELIVERY_STAGE_RETRY_SCHEDULED" ->
                    (status == ShipmentStatus.PROCESSING && "CREATE_ROUTE".equals(stage))
                    || (status == ShipmentStatus.IN_TRANSIT && "CONFIRM_DELIVERY".equals(stage));
            default -> false;
        };
        Instant deliveredAt = payload.deliveredAt();
        return consistent && (status == ShipmentStatus.DELIVERED
                ? deliveredAt != null && !deliveredAt.isAfter(timestamp) : deliveredAt == null);
    }

    public record Payload(
            @NotNull UUID taskId,
            @NotNull ShipmentStatus status,
            @NotNull @Pattern(regexp = "SEARCH_COURIER|CREATE_ROUTE|CONFIRM_DELIVERY") String currentStage,
            @NotBlank @Size(max = 1000) String details,
            @Nullable Instant deliveredAt) {
    }
}
