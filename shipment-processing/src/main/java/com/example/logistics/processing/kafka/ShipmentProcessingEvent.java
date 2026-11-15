package com.example.logistics.processing.kafka;

import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Событие для A: снимок результата перехода, а не ссылка на изменяемую задачу. */
public record ShipmentProcessingEvent(int schemaVersion, UUID eventId, UUID shipmentId, String eventType,
        Instant timestamp, Payload payload, UUID correlationId, long sequenceNumber) {

    public static ShipmentProcessingEvent from(ProcessingTaskEntity task, UUID eventId, String taskEventType,
            String details, Instant timestamp, long sequenceNumber) {
        String shipmentStatus = switch (task.getStatus()) {
            case QUEUED, PROCESSING -> "PROCESSING";
            case WAITING_FOR_COURIER -> "WAITING_FOR_COURIER";
            case IN_TRANSIT -> "IN_TRANSIT";
            case COMPLETED -> "DELIVERED";
            case FAILED -> "FAILED";
            case COURIER_NOT_FOUND -> "COURIER_NOT_FOUND";
        };
        String eventType = switch (taskEventType) {
            case "COURIER_SEARCH_STARTED", "COURIER_SEARCH_RECOVERED" -> "SHIPMENT_PROCESSING_STARTED";
            case "WAITING_FOR_COURIER" -> "SHIPMENT_WAITING_FOR_COURIER";
            case "TASK_FAILED" -> "SHIPMENT_FAILED";
            case "TASK_RETRY_REQUESTED" -> "SHIPMENT_PROCESSING_RESUMED";
            case "COURIER_ASSIGNED", "ROUTE_SHEET_CREATED", "SHIPMENT_IN_TRANSIT", "SHIPMENT_DELIVERED",
                    "COURIER_NOT_FOUND", "COURIER_SEARCH_RETRY_SCHEDULED", "DELIVERY_STAGE_RETRY_SCHEDULED" -> taskEventType;
            default -> throw new IllegalArgumentException("Unsupported task event type: " + taskEventType);
        };
        return new ShipmentProcessingEvent(1, eventId, task.getShipmentId(), eventType, timestamp,
                new Payload(task.getId(), shipmentStatus, task.getCurrentStage(), details,
                        "DELIVERED".equals(shipmentStatus) ? task.getCompletedAt() : null),
                task.getShipmentId(), sequenceNumber);
    }

    public record Payload(UUID taskId, String status, ProcessingStage currentStage, String details,
            @Nullable Instant deliveredAt) {
    }
}
