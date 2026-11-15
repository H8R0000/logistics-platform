package com.example.logistics.courier.service;

import com.example.logistics.courier.dto.CourierResponse;
import com.example.logistics.courier.dto.CouriersResponse;
import com.example.logistics.courier.entity.CourierEntity;
import com.example.logistics.courier.exception.CourierNotFoundException;
import com.example.logistics.courier.repository.CourierRepository;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class CourierService {

    private final CourierRepository couriers;

    public CourierService(CourierRepository couriers) {
        this.couriers = couriers;
    }

    public CouriersResponse list() {
        return new CouriersResponse(couriers.findAll(Sort.by("id")).stream()
                .map(this::toResponse)
                .toList());
    }

    public CourierResponse get(UUID id) {
        CourierEntity courier = couriers.findById(id)
                .orElseThrow(() -> new CourierNotFoundException(id));
        return toResponse(courier);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'COURIER_READER')")
    public CouriersResponse available() {
        return new CouriersResponse(couriers.findByAvailableTrueOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList());
    }

    private CourierResponse toResponse(CourierEntity courier) {
        return new CourierResponse(courier.getId(), courier.getName(), courier.getVehicleType(),
                courier.getMaxWeight(), courier.getCurrentLoad(), courier.isAvailable(), courier.getUpdatedAt());
    }
}
