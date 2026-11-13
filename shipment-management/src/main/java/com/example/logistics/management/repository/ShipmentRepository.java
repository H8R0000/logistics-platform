package com.example.logistics.management.repository;

import com.example.logistics.management.entity.ShipmentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, UUID>,
        JpaSpecificationExecutor<ShipmentEntity> {

    Optional<ShipmentEntity> findByIdAndOwnerId(UUID id, String ownerId);
}
