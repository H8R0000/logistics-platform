package com.example.logistics.processing.service;

import com.example.logistics.processing.config.ProcessingProperties;
import com.example.logistics.processing.entity.ProcessingStage;
import com.example.logistics.processing.entity.ProcessingTaskEntity;
import com.example.logistics.processing.entity.ProcessingTaskStatus;
import com.example.logistics.processing.repository.CourierAssignmentRepository;
import com.example.logistics.processing.repository.ProcessingTaskRepository;
import com.example.logistics.processing.workflow.ClaimedTask;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Выполняет короткие транзакции захвата задачи и сохранения результатов поиска вместе с историей.
 * Версия, статус, этап и срок выполнения проверяются перед применением результата worker.
 */
@Service
public class CourierSearchStateService {

    private static final Set<ProcessingTaskStatus> SEARCH_STATUSES = Set.of(
        ProcessingTaskStatus.QUEUED, ProcessingTaskStatus.WAITING_FOR_COURIER, ProcessingTaskStatus.PROCESSING);

    private final ProcessingTaskRepository processingTaskRepository;
    private final ProcessingTaskEventService processingTaskEventService;
    private final CourierAssignmentRepository courierAssignmentRepository;
    private final ProcessingProperties processingProperties;

    public CourierSearchStateService(ProcessingTaskRepository processingTaskRepository, ProcessingTaskEventService processingTaskEventService,
                                     CourierAssignmentRepository courierAssignmentRepository, ProcessingProperties processingProperties) {
        this.processingTaskRepository = processingTaskRepository;
        this.processingTaskEventService = processingTaskEventService;
        this.courierAssignmentRepository = courierAssignmentRepository;
        this.processingProperties = processingProperties;
    }

    /**
     * Блокирует строку и повторно проверяет готовность задачи к поиску курьера.
     * При захвате ставит PROCESSING и срок повторного запуска в nextExecutionAt;
     * после сбоя этот срок позволит другому worker продолжить задачу.
     *
     * @return снимок с версией после flush; пустой результат, если выполнение сейчас недопустимо
     */
    @Transactional
    public Optional<ClaimedTask> claim(UUID id) {
        ProcessingTaskEntity task = processingTaskRepository.findForExecution(id).orElse(null);
        Instant now = Instant.now();
        if (task == null || task.getCurrentStage() != ProcessingStage.SEARCH_COURIER
            || !SEARCH_STATUSES.contains(task.getStatus()) || task.getNextExecutionAt().isAfter(now)) {
            return Optional.empty();
        }
        if (expired(task, now)) {
            notFound(task, now);
            return Optional.empty();
        }
        boolean recovered = task.getStatus() == ProcessingTaskStatus.PROCESSING;
        task.startSearch(now, now.plus(processingProperties.executionTimeout()));
        saveTaskEvent(task, recovered ? "COURIER_SEARCH_RECOVERED" : "COURIER_SEARCH_STARTED",
            recovered ? "Previous execution expired; courier search resumed" : "Courier search started", now);
        processingTaskRepository.flush();
        return Optional.of(new ClaimedTask(id, task.getVersion(), task.getShipmentData()));
    }

    /**
     * Атомарно сохраняет назначение, переход к CREATE_ROUTE и историю.
     * Конкурирующее назначение курьера или заявки отклоняется уникальными индексами БД.
     *
     * @return ASSIGNED при успехе, BUSY при конфликте назначения, STALE если результат больше неприменим
     */
    @Transactional
    public AssignmentResult assign(ClaimedTask claim, UUID courierId) {
        Instant now = Instant.now();
        ProcessingTaskEntity task = owned(claim, now);
        if (task == null) {
            return AssignmentResult.STALE;
        }
        if (expired(task, now)) {
            notFound(task, now);
            return AssignmentResult.STALE;
        }
        if (courierAssignmentRepository.insertIfAvailable(UUID.randomUUID(), task.getShipmentId(), courierId, now) == 0) {
            return AssignmentResult.BUSY;
        }
        task.assignCourier(now);
        saveTaskEvent(task, "COURIER_ASSIGNED", "Courier " + courierId + " assigned; ready to create route", now);
        return AssignmentResult.ASSIGNED;
    }

    /**
     * Планирует следующую проверку при отсутствии подходящего курьера, сбрасывая технические ошибки.
     * По истечении общего срока поиска завершает задачу как COURIER_NOT_FOUND.
     */
    @Transactional
    public void waitForCourier(ClaimedTask claim) {
        Instant now = Instant.now();
        ProcessingTaskEntity task = owned(claim, now);
        if (task == null) {
            return;
        }
        if (expired(task, now)) {
            notFound(task, now);
            return;
        }
        task.waitForCourier(earlier(now.plus(processingProperties.courierSearchInterval()),
            task.getStartedAt().plus(processingProperties.maxCourierWait())));
        saveTaskEvent(task, "WAITING_FOR_COURIER", "No suitable courier without an active assignment", now);
    }

    /**
     * Учитывает техническую неудачу и сохраняет время повтора с exponential backoff, освобождая worker.
     * При достижении лимита ошибок завершает задачу как FAILED. Устаревший результат игнорируется.
     */
    @Transactional
    public void recordFailure(ClaimedTask claim, String details) {
        Instant now = Instant.now();
        ProcessingTaskEntity task = owned(claim, now);
        if (task == null) {
            return;
        }
        task.recordFailure(details);
        if (task.getAttempts() >= processingProperties.maxAttempts()) {
            task.fail(now);
            saveTaskEvent(task, "TASK_FAILED", details, now);
        } else if (expired(task, now)) {
            notFound(task, now);
        } else {
            ExponentialBackOff backOff = new ExponentialBackOff(processingProperties.retryInitialDelay().toMillis(), 2);
            backOff.setMaxInterval(processingProperties.retryMaxDelay().toMillis());
            BackOffExecution execution = backOff.start();
            long delay = 0;
            for (int i = 0; i < task.getAttempts(); i++) {
                delay = execution.nextBackOff();
            }
            task.scheduleRetry(earlier(now.plusMillis(delay),
                task.getStartedAt().plus(processingProperties.maxCourierWait())));
            saveTaskEvent(task, "COURIER_SEARCH_RETRY_SCHEDULED", details, now);
        }
    }

    private @Nullable ProcessingTaskEntity owned(ClaimedTask claim, Instant now) {
        ProcessingTaskEntity task = processingTaskRepository.findForExecution(claim.id()).orElse(null);
        if (task == null || task.getVersion() != claim.version()
            || task.getStatus() != ProcessingTaskStatus.PROCESSING
            || task.getCurrentStage() != ProcessingStage.SEARCH_COURIER
            || !task.getNextExecutionAt().isAfter(now)) {
            return null;
        }
        return task;
    }

    private boolean expired(ProcessingTaskEntity task, Instant now) {
        return task.getStartedAt() != null
            && !now.isBefore(task.getStartedAt().plus(processingProperties.maxCourierWait()));
    }

    private void notFound(ProcessingTaskEntity task, Instant now) {
        task.courierNotFound(now);
        saveTaskEvent(task, "COURIER_NOT_FOUND", "Maximum courier search time exceeded", now);
    }

    /** Сохраняет историю и исходящее событие в транзакции перехода задачи. */
    private void saveTaskEvent(ProcessingTaskEntity task, String eventType, String details, Instant createdAt) {
        processingTaskEventService.saveTaskEvent(task, eventType, details, createdAt);
    }

    private static Instant earlier(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    public enum AssignmentResult {
        ASSIGNED, BUSY, STALE
    }
}
