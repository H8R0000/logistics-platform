package com.example.logistics.processing.service;

import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.entity.ProcessingTaskEventEntity;
import com.example.logistics.processing.kafka.ShipmentProcessingEvent;
import com.example.logistics.processing.kafka.ShipmentProcessingError;
import com.example.logistics.processing.outbox.OutboxMessageEntity;
import com.example.logistics.processing.outbox.OutboxMessageRepository;
import com.example.logistics.processing.repository.ProcessingTaskEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Сохраняет историю и outbox в транзакции перехода задачи; отдельная транзакция здесь запрещена. */
@Service
public class ProcessingTaskEventService {

    private final ProcessingTaskEventRepository processingTaskEventRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public ProcessingTaskEventService(ProcessingTaskEventRepository processingTaskEventRepository,
            OutboxMessageRepository outboxMessageRepository, ObjectMapper objectMapper) {
        this.processingTaskEventRepository = processingTaskEventRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }

    /** Задача должна быть загружена под блокировкой строки в вызывающем сервисе. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveTaskEvent(ProcessingTaskEntity task, String eventType, String details, Instant now) {
        UUID eventId = UUID.randomUUID();
        long sequenceNumber = task.nextEventSequence();
        processingTaskEventRepository.save(new ProcessingTaskEventEntity(eventId, task.getId(), eventType,
                task.getStatus(), task.getCurrentStage(), details, now));
        ShipmentProcessingEvent event = ShipmentProcessingEvent.from(task, eventId, eventType,
                details, now, sequenceNumber);
        outboxMessageRepository.save(new OutboxMessageEntity(eventId, task.getShipmentId(), sequenceNumber,
                "shipment-events", objectMapper.writeValueAsString(event), now));
        if ("TASK_FAILED".equals(eventType)) {
            UUID errorId = UUID.randomUUID();
            ShipmentProcessingError error = ShipmentProcessingError.from(task, errorId, details, now, sequenceNumber);
            outboxMessageRepository.save(new OutboxMessageEntity(errorId, task.getShipmentId(), sequenceNumber,
                    "shipment-errors", objectMapper.writeValueAsString(error), now));
        }
    }
}
