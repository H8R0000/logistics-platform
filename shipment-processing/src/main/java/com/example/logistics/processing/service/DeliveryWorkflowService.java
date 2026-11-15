package com.example.logistics.processing.service;

import com.example.logistics.processing.config.DeliverySimulationProperties;
import com.example.logistics.processing.config.ProcessingProperties;
import com.example.logistics.processing.entity.AssignmentStatus;
import com.example.logistics.processing.entity.CourierAssignmentEntity;
import com.example.logistics.processing.entity.DeliveryType;
import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.entity.RouteSheetEntity;
import com.example.logistics.processing.entity.RouteSheetStatus;
import com.example.logistics.processing.entity.ShipmentData;
import com.example.logistics.processing.repository.CourierAssignmentRepository;
import com.example.logistics.processing.repository.ProcessingTaskRepository;
import com.example.logistics.processing.repository.RouteSheetRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Продвигает доставку короткими транзакциями БД: создание маршрута и подтверждение доставки.
 * Блокировка задачи и проверка этапа делают повторный запуск безопасным между экземплярами B.
 */
@Service
public class DeliveryWorkflowService {

    private final ProcessingTaskRepository processingTaskRepository;
    private final ProcessingTaskEventService processingTaskEventService;
    private final CourierAssignmentRepository courierAssignmentRepository;
    private final RouteSheetRepository routeSheetRepository;
    private final DeliverySimulationProperties deliverySimulationProperties;
    private final ProcessingProperties processingProperties;

    public DeliveryWorkflowService(ProcessingTaskRepository processingTaskRepository,
            ProcessingTaskEventService processingTaskEventService,
            CourierAssignmentRepository courierAssignmentRepository, RouteSheetRepository routeSheetRepository,
            DeliverySimulationProperties deliverySimulationProperties, ProcessingProperties processingProperties) {
        this.processingTaskRepository = processingTaskRepository;
        this.processingTaskEventService = processingTaskEventService;
        this.courierAssignmentRepository = courierAssignmentRepository;
        this.routeSheetRepository = routeSheetRepository;
        this.deliverySimulationProperties = deliverySimulationProperties;
        this.processingProperties = processingProperties;
    }

    /** Проверяет версию, этап и время под блокировкой строки; одна транзакция — один переход. */
    @Transactional
    public void advanceDueDelivery(UUID taskId, long expectedVersion) {
        ProcessingTaskEntity task = processingTaskRepository.findForExecution(taskId).orElse(null);
        Instant now = Instant.now();
        if (task == null || task.getVersion() != expectedVersion || task.getNextExecutionAt().isAfter(now)) {
            return;
        }
        if (task.getStatus() == ProcessingTaskStatus.PROCESSING
                && task.getCurrentStage() == ProcessingStage.CREATE_ROUTE) {
            createRouteAndStartDelivery(task, now);
        } else if (task.getStatus() == ProcessingTaskStatus.IN_TRANSIT
                && task.getCurrentStage() == ProcessingStage.CONFIRM_DELIVERY) {
            confirmDelivery(task, now);
        }
    }

    private void createRouteAndStartDelivery(ProcessingTaskEntity task, Instant now) {
        CourierAssignmentEntity assignment = courierAssignmentRepository
                .findByShipmentIdAndStatus(task.getShipmentId(), AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Active courier assignment is missing"));
        if (routeSheetRepository.findByShipmentId(task.getShipmentId()).isPresent()) {
            throw new IllegalStateException("Route already exists for a task awaiting route creation");
        }
        ShipmentData data = task.getShipmentData();
        RouteSheetEntity route = routeSheetRepository.save(new RouteSheetEntity(UUID.randomUUID(),
                task.getShipmentId(), assignment.getCourierId(), data.origin(), data.destination(),
                calculateDuration(data.deliveryType()), now));
        saveTaskEvent(task, "ROUTE_SHEET_CREATED", "Route " + route.getId() + " created", now);
        task.startDelivery(route.getEstimatedDeliveryAt());
        saveTaskEvent(task, "SHIPMENT_IN_TRANSIT",
                "Delivery started; expected at " + route.getEstimatedDeliveryAt(), now);
    }

    private void confirmDelivery(ProcessingTaskEntity task, Instant now) {
        RouteSheetEntity route = routeSheetRepository.findByShipmentId(task.getShipmentId())
                .orElseThrow(() -> new IllegalStateException("Route is missing for delivery"));
        if (route.getStatus() != RouteSheetStatus.IN_TRANSIT) {
            throw new IllegalStateException("Route must be IN_TRANSIT before confirmation");
        }
        if (route.getEstimatedDeliveryAt().isAfter(now)) {
            task.scheduleNextExecution(route.getEstimatedDeliveryAt());
            return;
        }
        if (courierAssignmentRepository.releaseActiveAssignment(task.getShipmentId(), route.getCourierId()) != 1) {
            throw new IllegalStateException("Active assignment does not match the route");
        }
        // По ТЗ подтверждение курьера имитируется: дополнительных HTTP-запросов здесь нет.
        route.markDelivered();
        task.completeDelivery(now);
        saveTaskEvent(task, "SHIPMENT_DELIVERED", "Delivery confirmed; courier released", now);
    }

    /** Сохраняет backoff; версия отбрасывает ошибку старого запуска после прогресса задачи или ручного retry. */
    @Transactional
    public void recordStageFailure(UUID taskId, ProcessingStage expectedStage, long expectedVersion, String details) {
        ProcessingTaskEntity task = processingTaskRepository.findForExecution(taskId).orElse(null);
        Instant now = Instant.now();
        if (task == null || task.getVersion() != expectedVersion || task.getCurrentStage() != expectedStage
                || !((expectedStage == ProcessingStage.CREATE_ROUTE && task.getStatus() == ProcessingTaskStatus.PROCESSING)
                || (expectedStage == ProcessingStage.CONFIRM_DELIVERY && task.getStatus() == ProcessingTaskStatus.IN_TRANSIT))) {
            return;
        }
        task.recordFailure(details);
        if (task.getAttempts() >= processingProperties.maxAttempts()) {
            task.fail(now);
            saveTaskEvent(task, "TASK_FAILED", details, now);
        } else {
            ExponentialBackOff backOff = new ExponentialBackOff(processingProperties.retryInitialDelay().toMillis(), 2);
            backOff.setMaxInterval(processingProperties.retryMaxDelay().toMillis());
            BackOffExecution execution = backOff.start();
            long delay = 0;
            for (int i = 0; i < task.getAttempts(); i++) {
                delay = execution.nextBackOff();
            }
            task.scheduleNextExecution(now.plusMillis(delay));
            saveTaskEvent(task, "DELIVERY_STAGE_RETRY_SCHEDULED", details, now);
        }
    }

    private Duration calculateDuration(DeliveryType deliveryType) {
        boolean express = deliveryType == DeliveryType.EXPRESS;
        long min = (express ? deliverySimulationProperties.expressMinDuration()
                : deliverySimulationProperties.standardMinDuration()).toMillis();
        long max = (express ? deliverySimulationProperties.expressMaxDuration()
                : deliverySimulationProperties.standardMaxDuration()).toMillis();
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(min, max + 1));
    }

    private void saveTaskEvent(ProcessingTaskEntity task, String eventType, String details, Instant now) {
        processingTaskEventService.saveTaskEvent(task, eventType, details, now);
    }
}
