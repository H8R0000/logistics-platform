package com.example.logistics.management.outbox;

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

    @Query("select m.id from OutboxMessageEntity m where m.publishedAt is null order by m.createdAt, m.id")
    List<UUID> findPendingIds(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from OutboxMessageEntity m where m.id = :id")
    Optional<OutboxMessageEntity> findForPublication(@Param("id") UUID id);
}
