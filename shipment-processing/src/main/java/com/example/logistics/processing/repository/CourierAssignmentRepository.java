package com.example.logistics.processing.repository;

import com.example.logistics.processing.entity.CourierAssignmentEntity;
import com.example.logistics.processing.entity.AssignmentStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourierAssignmentRepository extends JpaRepository<CourierAssignmentEntity, UUID> {

    @Query("select a.courierId from CourierAssignmentEntity a where a.status = :status")
    List<UUID> findCourierIdsByStatus(@Param("status") AssignmentStatus status);

    Optional<CourierAssignmentEntity> findByShipmentIdAndStatus(UUID shipmentId, AssignmentStatus status);

    /** Освобождает только назначение выбранной заявки и курьера, в транзакции завершения доставки. */
    @Modifying
    @Query(value = """
        UPDATE courier_assignments SET status = 'RELEASED'
        WHERE shipment_id = :shipmentId AND courier_id = :courierId AND status = 'ACTIVE'
        """, nativeQuery = true)
    int releaseActiveAssignment(@Param("shipmentId") UUID shipmentId, @Param("courierId") UUID courierId);

    /**
     * Пытается создать активное назначение; уникальные индексы защищают курьера и заявку от двойного назначения.
     *
     * @return 1 при вставке, 0 при конфликте уникальности
     */
    @Modifying
    @Query(value = """
        INSERT INTO courier_assignments (id, shipment_id, courier_id, assigned_at, status)
        VALUES (:id, :shipmentId, :courierId, :assignedAt, 'ACTIVE')
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int insertIfAvailable(@Param("id") UUID id, @Param("shipmentId") UUID shipmentId,
                          @Param("courierId") UUID courierId, @Param("assignedAt") Instant assignedAt);
}
