package com.example.logistics.processing.kafka;

import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import java.time.Instant;
import java.util.UUID;

/** Конечная техническая ошибка задачи; сообщение хранится в outbox вместе с переходом в FAILED. */
public record ShipmentProcessingError(int schemaVersion, UUID eventId, UUID shipmentId, String eventType,
        Instant timestamp, Payload payload, UUID correlationId, long sequenceNumber) {

    public static ShipmentProcessingError from(ProcessingTaskEntity task, UUID eventId,
            String details, Instant timestamp, long sequenceNumber) {
        return new ShipmentProcessingError(1, eventId, task.getShipmentId(), "SHIPMENT_PROCESSING_FAILED",
                timestamp, new Payload(task.getId(), task.getCurrentStage(), task.getAttempts(), details),
                task.getShipmentId(), sequenceNumber);
    }

    public record Payload(UUID taskId, ProcessingStage currentStage, int attempts, String details) {
    }
}
