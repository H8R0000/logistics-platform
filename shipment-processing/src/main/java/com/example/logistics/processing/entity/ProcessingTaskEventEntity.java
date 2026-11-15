package com.example.logistics.processing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_task_events")
public class ProcessingTaskEventEntity {

    @Id
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProcessingTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 32)
    private ProcessingStage currentStage;

    @Column(nullable = false, length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProcessingTaskEventEntity() {
    }

    public ProcessingTaskEventEntity(UUID id, UUID taskId, String eventType, ProcessingTaskStatus status,
                                     ProcessingStage currentStage, String details, Instant createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.eventType = eventType;
        this.status = status;
        this.currentStage = currentStage;
        this.details = details;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getEventType() {
        return eventType;
    }

    public ProcessingTaskStatus getStatus() {
        return status;
    }

    public ProcessingStage getCurrentStage() {
        return currentStage;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
