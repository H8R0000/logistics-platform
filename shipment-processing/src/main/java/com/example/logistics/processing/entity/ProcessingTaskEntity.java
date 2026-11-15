package com.example.logistics.processing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "processing_tasks")
public class ProcessingTaskEntity {

    @Id
    private UUID id;

    @Column(name = "shipment_id", nullable = false, unique = true)
    private UUID shipmentId;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipment_data", nullable = false, columnDefinition = "jsonb")
    private ShipmentData shipmentData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProcessingTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 32)
    private ProcessingStage currentStage;

    /** Последовательные технические неудачи текущего этапа; успешная операция сбрасывает счётчик. */
    @Column(nullable = false)
    private int attempts;

    /** Порядок исходящих событий заявки; изменяется под блокировкой строки задачи. */
    @Column(name = "event_sequence", nullable = false)
    private long eventSequence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Начало текущего цикла поиска; ручной retry поиска начинает новый срок ожидания. */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Время следующей попытки или проверки. Для PROCESSING/SEARCH_COURIER — срок текущего выполнения,
     * после которого разрешён повторный захват задачи для восстановления.
     */
    @Column(name = "next_execution_at", nullable = false)
    private Instant nextExecutionAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /** Версия JPA; снимок после захвата позволяет отклонить запоздавший результат старого worker. */
    @Version
    @Column(nullable = false)
    private Long version;

    protected ProcessingTaskEntity() {
    }

    public ProcessingTaskEntity(UUID id, UUID shipmentId, String ownerId,
                                ShipmentData shipmentData, Instant createdAt) {
        this.id = id;
        this.shipmentId = shipmentId;
        this.ownerId = ownerId;
        this.shipmentData = shipmentData;
        this.status = ProcessingTaskStatus.QUEUED;
        this.currentStage = ProcessingStage.SEARCH_COURIER;
        this.createdAt = createdAt;
        this.nextExecutionAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShipmentId() {
        return shipmentId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public ShipmentData getShipmentData() {
        return shipmentData;
    }

    public ProcessingTaskStatus getStatus() {
        return status;
    }

    public ProcessingStage getCurrentStage() {
        return currentStage;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getNextExecutionAt() {
        return nextExecutionAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getVersion() {
        return version;
    }

    public long nextEventSequence() {
        return ++eventSequence;
    }

    public void startSearch(Instant now, Instant executionDeadline) {
        if (startedAt == null) {
            startedAt = now;
        }
        status = ProcessingTaskStatus.PROCESSING;
        nextExecutionAt = executionDeadline;
    }

    public void assignCourier(Instant now) {
        currentStage = ProcessingStage.CREATE_ROUTE;
        attempts = 0;
        errorMessage = null;
        nextExecutionAt = now;
    }

    public void waitForCourier(Instant nextCheck) {
        status = ProcessingTaskStatus.WAITING_FOR_COURIER;
        attempts = 0;
        errorMessage = null;
        nextExecutionAt = nextCheck;
    }

    /** Начало доставки и время отложенного подтверждения сохраняются вместе с маршрутным листом. */
    public void startDelivery(Instant estimatedDeliveryAt) {
        status = ProcessingTaskStatus.IN_TRANSIT;
        currentStage = ProcessingStage.CONFIRM_DELIVERY;
        attempts = 0;
        errorMessage = null;
        nextExecutionAt = estimatedDeliveryAt;
    }

    public void scheduleNextExecution(Instant nextExecutionAt) {
        this.nextExecutionAt = nextExecutionAt;
    }

    /** Вызывается в общей транзакции с завершением маршрута, освобождением курьера и историей. */
    public void completeDelivery(Instant now) {
        status = ProcessingTaskStatus.COMPLETED;
        attempts = 0;
        errorMessage = null;
        completedAt = now;
    }

    public void recordFailure(String errorMessage) {
        attempts++;
        this.errorMessage = errorMessage;
    }

    public void scheduleRetry(Instant nextAttempt) {
        status = ProcessingTaskStatus.QUEUED;
        nextExecutionAt = nextAttempt;
    }

    public void fail(Instant now) {
        status = ProcessingTaskStatus.FAILED;
        completedAt = now;
    }

    public void courierNotFound(Instant now) {
        status = ProcessingTaskStatus.COURIER_NOT_FOUND;
        attempts = 0;
        errorMessage = null;
        completedAt = now;
    }

    /** Возобновляет проверенный сервисом этап; история, назначение и счётчик событий сохраняются. */
    public void prepareForRetry(Instant now) {
        status = switch (currentStage) {
            case SEARCH_COURIER -> {
                startedAt = null;
                yield ProcessingTaskStatus.QUEUED;
            }
            case CREATE_ROUTE -> ProcessingTaskStatus.PROCESSING;
            case CONFIRM_DELIVERY -> ProcessingTaskStatus.IN_TRANSIT;
        };
        attempts = 0;
        errorMessage = null;
        completedAt = null;
        nextExecutionAt = now;
    }
}
