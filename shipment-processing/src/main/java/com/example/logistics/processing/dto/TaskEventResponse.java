package com.example.logistics.processing.dto;

import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskEventResponse(UUID id, UUID taskId, String eventType, ProcessingTaskStatus status,
                                ProcessingStage currentStage, String details, Instant createdAt) {
}
