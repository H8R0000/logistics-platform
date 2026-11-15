package com.example.logistics.management.service;

import com.example.logistics.management.dto.CreateShipmentRequest;
import com.example.logistics.management.dto.PageResponse;
import com.example.logistics.management.dto.ShipmentAcceptedResponse;
import com.example.logistics.management.dto.ShipmentEventResponse;
import com.example.logistics.management.dto.ShipmentResponse;
import com.example.logistics.management.entity.ShipmentEntity;
import com.example.logistics.management.exception.ShipmentNotFoundException;
import com.example.logistics.management.mapper.ShipmentMapper;
import com.example.logistics.management.repository.ShipmentEventRepository;
import com.example.logistics.management.repository.ShipmentRepository;
import com.example.logistics.management.repository.ShipmentSpecifications;
import com.example.logistics.management.security.CurrentUser;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class ShipmentService {

    private final ShipmentRepository shipments;
    private final ShipmentEventRepository events;
    private final ShipmentMapper mapper;
    private final ShipmentRegistrationService shipmentRegistrationService;

    public ShipmentService(ShipmentRepository shipments, ShipmentEventRepository events, ShipmentMapper mapper,
                           ShipmentRegistrationService shipmentRegistrationService) {
        this.shipments = shipments;
        this.events = events;
        this.mapper = mapper;
        this.shipmentRegistrationService = shipmentRegistrationService;
    }

    @Transactional
    public ShipmentAcceptedResponse create(CreateShipmentRequest request, CurrentUser currentUser) {
        return shipmentRegistrationService.register(request, currentUser.id());
    }

    public ShipmentResponse get(UUID id, CurrentUser currentUser) {
        return mapper.toResponse(findAccessible(id, currentUser));
    }

    public PageResponse<ShipmentResponse> list(ShipmentSearchFilter filter, CurrentUser currentUser) {
        return PageResponse.from(shipments.findAll(ShipmentSpecifications.matching(filter, currentUser), filter.pageable()).map(mapper::toResponse));
    }

    public List<ShipmentEventResponse> history(UUID id, CurrentUser currentUser) {
        findAccessible(id, currentUser);
        return events.findByShipmentIdOrderByProcessingSequenceAscCreatedAtAscIdAsc(id).stream().map(mapper::toEventResponse).toList();
    }

    private ShipmentEntity findAccessible(UUID id, CurrentUser currentUser) {
        // Return the same 404 for an absent shipment and another user's shipment.
        return (currentUser.admin() ? shipments.findById(id) : shipments.findByIdAndOwnerId(id, currentUser.id())).orElseThrow(() -> new ShipmentNotFoundException(id));
    }
}
