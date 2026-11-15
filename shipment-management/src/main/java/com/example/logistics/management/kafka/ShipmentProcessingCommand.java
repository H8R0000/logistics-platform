package com.example.logistics.management.kafka;

import com.example.logistics.management.entity.Address;
import com.example.logistics.management.entity.DeliveryType;
import com.example.logistics.management.entity.Dimensions;
import com.example.logistics.management.entity.ShipmentEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShipmentProcessingCommand(int schemaVersion, UUID messageId, UUID shipmentId,
                                        UUID correlationId, Instant createdAt, String ownerId,
                                        ShipmentData shipmentData) {

    public static ShipmentProcessingCommand from(ShipmentEntity shipment, UUID messageId) {
        return new ShipmentProcessingCommand(1, messageId, shipment.getId(), shipment.getId(),
            shipment.getCreatedAt(), shipment.getOwnerId(),
            new ShipmentData(shipment.getOrigin(), shipment.getDestination(), shipment.getWeight(),
                shipment.getDimensions(), shipment.getDeliveryType()));
    }

    public record ShipmentData(Address origin, Address destination, BigDecimal weight,
                               Dimensions dimensions, DeliveryType deliveryType) {
    }
}
