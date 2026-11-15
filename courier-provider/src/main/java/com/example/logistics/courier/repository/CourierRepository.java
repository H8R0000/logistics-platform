package com.example.logistics.courier.repository;

import com.example.logistics.courier.entity.CourierEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierRepository extends JpaRepository<CourierEntity, UUID> {

    List<CourierEntity> findByAvailableTrueOrderByIdAsc();
}
