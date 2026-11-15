package com.example.logistics.management.repository;

import com.example.logistics.management.entity.ShipmentEntity;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID>,
        JpaSpecificationExecutor<ShipmentEntity> {

    Optional<ShipmentEntity> findByIdAndOwnerId(UUID id, String ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShipmentEntity s where s.id = :id")
    Optional<ShipmentEntity> findForUpdate(@Param("id") UUID id);

    /** @return 1 при создании заявки, 0 при повторе sourceId/sourceMessageId; история и outbox пишутся в той же транзакции. */
    @Modifying
    @Query(value = """
            INSERT INTO shipments (id, owner_id, source_id, source_message_id, origin, destination,
                                   weight, dimensions, delivery_type, status, created_at, updated_at, version)
            VALUES (:id, :ownerId, :sourceId, :sourceMessageId, CAST(:origin AS jsonb), CAST(:destination AS jsonb),
                    :weight, CAST(:dimensions AS jsonb), :deliveryType, 'CREATED', :now, :now, 0)
            ON CONFLICT (source_id, source_message_id) DO NOTHING
            """, nativeQuery = true)
    int insertExternalIfAbsent(@Param("id") UUID id, @Param("ownerId") String ownerId,
            @Param("sourceId") String sourceId, @Param("sourceMessageId") UUID sourceMessageId,
            @Param("origin") String origin, @Param("destination") String destination,
            @Param("weight") BigDecimal weight, @Param("dimensions") String dimensions,
            @Param("deliveryType") String deliveryType, @Param("now") Instant now);
}
