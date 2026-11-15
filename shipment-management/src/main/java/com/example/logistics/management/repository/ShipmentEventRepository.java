package com.example.logistics.management.repository;

import com.example.logistics.management.entity.ShipmentEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEventEntity, UUID> {

    List<ShipmentEventEntity> findByShipmentIdOrderByProcessingSequenceAscCreatedAtAscIdAsc(UUID shipmentId);
}
