package com.example.logistics.processing.service;

import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskEventEntity;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.kafka.ShipmentProcessingCommand;
import com.example.logistics.processing.repository.ProcessingTaskEventRepository;
import com.example.logistics.processing.repository.ProcessingTaskRepository;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Регистрирует задачу из Kafka-команды вместе с историей в одной транзакции.
 * Повтор для существующего shipmentId завершается без изменений.
 */
@Service
public class ProcessingTaskRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingTaskRegistrationService.class);

    private final ProcessingTaskRepository tasks;
    private final ProcessingTaskEventRepository events;
    private final ObjectMapper objectMapper;

    public ProcessingTaskRegistrationService(ProcessingTaskRepository tasks, ProcessingTaskEventRepository events, ObjectMapper objectMapper) {
        this.tasks = tasks;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void register(ShipmentProcessingCommand command) {
        Instant now = Instant.now();
        UUID taskId = UUID.randomUUID();
        int inserted = tasks.insertIfAbsent(taskId, command.shipmentId(), command.ownerId(), objectMapper.writeValueAsString(command.shipmentData()), now);
        // защита от дублей
        if (inserted == 0) {
            log.info("Task already exists for shipment {}; command {}", command.shipmentId(), command.messageId());
            return;
        }
        events.save(new ProcessingTaskEventEntity(UUID.randomUUID(), taskId, "TASK_REGISTERED", ProcessingTaskStatus.QUEUED, ProcessingStage.SEARCH_COURIER, "Processing task registered", now));
        log.info("Registered task {} for shipment {}; message {}; correlation {}", taskId, command.shipmentId(), command.messageId(), command.correlationId());
    }
}
