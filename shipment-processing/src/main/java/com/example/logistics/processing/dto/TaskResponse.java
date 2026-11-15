package com.example.logistics.processing.dto;

import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.entity.ShipmentData;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record TaskResponse(UUID id, UUID shipmentId, String ownerId, ShipmentData shipmentData,
                           ProcessingTaskStatus status, ProcessingStage currentStage, int attempts,
                           Instant createdAt, @Nullable Instant startedAt, Instant nextExecutionAt,
                           @Nullable Instant completedAt, @Nullable String errorMessage) {
}
