package com.example.logistics.processing.repository;

import com.example.logistics.processing.entity.RouteSheetEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteSheetRepository extends JpaRepository<RouteSheetEntity, UUID> {
    Optional<RouteSheetEntity> findByShipmentId(UUID shipmentId);
}
