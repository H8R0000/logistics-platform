package com.example.logistics.processing.outbox;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, UUID> {

    /** Первое неопубликованное событие заявки в выбранном topic; порядок между разными topics не задаётся. */
    @Query("""
            select m.id from OutboxMessageEntity m
            where m.topic = :topic and m.publishedAt is null and not exists (
                select earlier.id from OutboxMessageEntity earlier
                where earlier.shipmentId = m.shipmentId and earlier.topic = m.topic and earlier.publishedAt is null
                    and earlier.sequenceNumber < m.sequenceNumber
            )
            order by m.createdAt, m.id
            """)
    List<UUID> findPublishableIds(@Param("topic") String topic, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from OutboxMessageEntity m where m.id = :id")
    Optional<OutboxMessageEntity> findForPublication(@Param("id") UUID id);
}
