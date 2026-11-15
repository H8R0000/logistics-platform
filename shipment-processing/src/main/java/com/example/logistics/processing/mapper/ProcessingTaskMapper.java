package com.example.logistics.processing.mapper;

import com.example.logistics.processing.dto.TaskEventResponse;
import com.example.logistics.processing.dto.TaskResponse;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.entity.ProcessingTaskEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessingTaskMapper {

    public TaskResponse toResponse(ProcessingTaskEntity task) {
        return new TaskResponse(task.getId(), task.getShipmentId(), task.getOwnerId(), task.getShipmentData(),
                task.getStatus(), task.getCurrentStage(), task.getAttempts(), task.getCreatedAt(),
                task.getStartedAt(), task.getNextExecutionAt(), task.getCompletedAt(), task.getErrorMessage());
    }

    public TaskEventResponse toEventResponse(ProcessingTaskEventEntity event) {
        return new TaskEventResponse(event.getId(), event.getTaskId(), event.getEventType(), event.getStatus(),
                event.getCurrentStage(), event.getDetails(), event.getCreatedAt());
    }
}
