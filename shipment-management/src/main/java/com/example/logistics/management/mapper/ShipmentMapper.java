package com.example.logistics.management.mapper;

import com.example.logistics.management.dto.AddressDto;
import com.example.logistics.management.dto.CreateShipmentRequest;
import com.example.logistics.management.dto.DimensionsDto;
import com.example.logistics.management.dto.ShipmentEventResponse;
import com.example.logistics.management.dto.ShipmentResponse;
import com.example.logistics.management.entity.Address;
import com.example.logistics.management.entity.Dimensions;
import com.example.logistics.management.entity.ShipmentEntity;
import com.example.logistics.management.entity.ShipmentEventEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShipmentMapper {

    public ShipmentEntity toEntity(CreateShipmentRequest request, UUID id, String ownerId, Instant createdAt) {
        return new ShipmentEntity(id, ownerId,
                toAddress(request.origin()), toAddress(request.destination()), request.weight(),
                new Dimensions(request.dimensions().length(), request.dimensions().width(),
                        request.dimensions().height()),
                request.deliveryType(), createdAt);
    }

    public ShipmentResponse toResponse(ShipmentEntity shipment) {
        Dimensions dimensions = shipment.getDimensions();
        return new ShipmentResponse(shipment.getId(), shipment.getOwnerId(),
                toDto(shipment.getOrigin()), toDto(shipment.getDestination()),
                shipment.getWeight(),
                new DimensionsDto(dimensions.getLength(), dimensions.getWidth(), dimensions.getHeight()),
                shipment.getDeliveryType(), shipment.getStatus(), shipment.getCreatedAt(),
                shipment.getUpdatedAt(), shipment.getDeliveredAt());
    }

    public ShipmentEventResponse toEventResponse(ShipmentEventEntity event) {
        return new ShipmentEventResponse(event.getId(), event.getShipmentId(), event.getEventType(),
                event.getStatus(), event.getDetails(), event.getCreatedAt());
    }

    private Address toAddress(AddressDto address) {
        return new Address(address.city(), address.country(), address.address());
    }

    private AddressDto toDto(Address address) {
        return new AddressDto(address.getCity(), address.getCountry(), address.getAddress());
    }
}
