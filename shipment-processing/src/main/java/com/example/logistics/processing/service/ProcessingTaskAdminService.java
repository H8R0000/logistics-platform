package com.example.logistics.processing.service;

import com.example.logistics.processing.dto.TaskResponse;
import com.example.logistics.processing.dto.TaskStatisticsResponse;
import com.example.logistics.processing.entity.AssignmentStatus;
import com.example.logistics.processing.entity.CourierAssignmentEntity;
import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.entity.RouteSheetEntity;
import com.example.logistics.processing.entity.RouteSheetStatus;
import com.example.logistics.processing.exception.TaskNotFoundException;
import com.example.logistics.processing.exception.TaskRetryConflictException;
import com.example.logistics.processing.mapper.ProcessingTaskMapper;
import com.example.logistics.processing.repository.CourierAssignmentRepository;
import com.example.logistics.processing.repository.ProcessingTaskRepository;
import com.example.logistics.processing.repository.RouteSheetRepository;
import com.example.logistics.processing.repository.TaskStatusCount;
import com.example.logistics.processing.security.CurrentUser;
import java.time.Instant;
import java.util.EnumMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Административные операции: общая статистика и возобновление остановившейся задачи. */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('ADMIN')")
public class ProcessingTaskAdminService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingTaskAdminService.class);

    private final ProcessingTaskRepository processingTaskRepository;
    private final CourierAssignmentRepository courierAssignmentRepository;
    private final RouteSheetRepository routeSheetRepository;
    private final ProcessingTaskEventService processingTaskEventService;
    private final ProcessingTaskMapper processingTaskMapper;

    public ProcessingTaskAdminService(ProcessingTaskRepository processingTaskRepository,
            CourierAssignmentRepository courierAssignmentRepository, RouteSheetRepository routeSheetRepository,
            ProcessingTaskEventService processingTaskEventService, ProcessingTaskMapper processingTaskMapper) {
        this.processingTaskRepository = processingTaskRepository;
        this.courierAssignmentRepository = courierAssignmentRepository;
        this.routeSheetRepository = routeSheetRepository;
        this.processingTaskEventService = processingTaskEventService;
        this.processingTaskMapper = processingTaskMapper;
    }

    /** Один запрос даёт согласованные счётчики; статусы без задач также возвращаются с нулём. */
    public TaskStatisticsResponse statistics() {
        EnumMap<ProcessingTaskStatus, Long> byStatus = new EnumMap<>(ProcessingTaskStatus.class);
        for (ProcessingTaskStatus status : ProcessingTaskStatus.values()) {
            byStatus.put(status, 0L);
        }
        long total = 0;
        for (TaskStatusCount row : processingTaskRepository.countTasksByStatus()) {
            byStatus.put(row.getStatus(), row.getCount());
            total += row.getCount();
        }
        return new TaskStatisticsResponse(total, byStatus);
    }

    /** Проверка, изменение задачи, история и outbox выполняются под одной блокировкой и транзакцией. */
    @Transactional
    public TaskResponse retry(UUID id, CurrentUser currentUser) {
        ProcessingTaskEntity task = processingTaskRepository.findForExecution(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        ProcessingTaskStatus previousStatus = task.getStatus();
        if (previousStatus != ProcessingTaskStatus.FAILED && previousStatus != ProcessingTaskStatus.COURIER_NOT_FOUND) {
            throw new TaskRetryConflictException("Only FAILED or COURIER_NOT_FOUND tasks can be retried");
        }
        validateRetryResources(task);
        Instant now = Instant.now();
        task.prepareForRetry(now);
        processingTaskEventService.saveTaskEvent(task, "TASK_RETRY_REQUESTED",
                "Retry requested by admin " + currentUser.id() + "; previous status " + previousStatus
                        + "; resume stage " + task.getCurrentStage(), now);
        log.info("Admin {} requested retry of task {} from {} at stage {}",
                currentUser.id(), id, previousStatus, task.getCurrentStage());
        return processingTaskMapper.toResponse(task);
    }

    /** Не исправляет противоречивые данные автоматически: возвращает причину конфликта администратору. */
    private void validateRetryResources(ProcessingTaskEntity task) {
        if (task.getStatus() == ProcessingTaskStatus.COURIER_NOT_FOUND
                && task.getCurrentStage() != ProcessingStage.SEARCH_COURIER) {
            throw new TaskRetryConflictException("COURIER_NOT_FOUND task must be at SEARCH_COURIER stage");
        }
        CourierAssignmentEntity assignment = courierAssignmentRepository
                .findByShipmentIdAndStatus(task.getShipmentId(), AssignmentStatus.ACTIVE).orElse(null);
        RouteSheetEntity route = routeSheetRepository.findByShipmentId(task.getShipmentId()).orElse(null);
        switch (task.getCurrentStage()) {
            case SEARCH_COURIER -> {
                if (assignment != null || route != null) {
                    throw new TaskRetryConflictException("Courier search requires no active assignment or route");
                }
            }
            case CREATE_ROUTE -> {
                if (assignment == null || route != null) {
                    throw new TaskRetryConflictException("Route creation requires an active assignment and no existing route");
                }
            }
            case CONFIRM_DELIVERY -> {
                if (assignment == null || route == null || route.getStatus() != RouteSheetStatus.IN_TRANSIT
                        || !route.getCourierId().equals(assignment.getCourierId())) {
                    throw new TaskRetryConflictException("Delivery confirmation requires an IN_TRANSIT route and its active courier assignment");
                }
            }
        }
    }
}
