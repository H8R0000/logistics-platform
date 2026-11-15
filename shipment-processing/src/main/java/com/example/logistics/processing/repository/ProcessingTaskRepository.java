package com.example.logistics.processing.repository;

import com.example.logistics.processing.entity.ProcessingTaskEntity;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessingTaskRepository extends JpaRepository<ProcessingTaskEntity, UUID>, JpaSpecificationExecutor<ProcessingTaskEntity> {

    Optional<ProcessingTaskEntity> findByIdAndOwnerId(UUID id, String ownerId);

    @Query("select t.status as status, count(t) as count from ProcessingTaskEntity t group by t.status")
    List<TaskStatusCount> countTasksByStatus();

    /** Выбирает готовые этапы; время доставки и восстановления поиска хранится в nextExecutionAt. */
    @Query(value = """
        SELECT id FROM processing_tasks
        WHERE next_execution_at <= :now
          AND ((current_stage = 'SEARCH_COURIER' AND status IN ('QUEUED', 'WAITING_FOR_COURIER', 'PROCESSING'))
            OR (current_stage = 'CREATE_ROUTE' AND status = 'PROCESSING')
            OR (current_stage = 'CONFIRM_DELIVERY' AND status = 'IN_TRANSIT'))
        ORDER BY next_execution_at, id
        """, nativeQuery = true)
    List<UUID> findDueTaskIds(@Param("now") Instant now, Pageable pageable);

    /** Этап и версия позволяют пропустить устаревший запуск и его ошибку после ручного retry. */
    @Query("select t.currentStage as currentStage, t.version as version from ProcessingTaskEntity t where t.id = :id")
    Optional<TaskExecutionSnapshot> findExecutionSnapshotById(@Param("id") UUID id);

    /** Загружает задачу с блокировкой строки до конца транзакции вызывающего сервиса. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ProcessingTaskEntity t where t.id = :id")
    Optional<ProcessingTaskEntity> findForExecution(@Param("id") UUID id);

    /**
     * Создаёт одну задачу на shipmentId; применяется в общей транзакции с записью истории.
     *
     * @return 1 при вставке, 0 если задача для shipmentId уже существует
     */
    @Modifying
    @Query(value = """
        INSERT INTO processing_tasks (id, shipment_id, owner_id, shipment_data, status, current_stage,
                                      attempts, created_at, next_execution_at, version)
        VALUES (:id, :shipmentId, :ownerId, CAST(:shipmentData AS jsonb), 'QUEUED', 'SEARCH_COURIER',
                0, :createdAt, :createdAt, 0)
        ON CONFLICT (shipment_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("shipmentId") UUID shipmentId, @Param("ownerId") String ownerId, @Param("shipmentData") String shipmentData, @Param("createdAt") Instant createdAt);
}
