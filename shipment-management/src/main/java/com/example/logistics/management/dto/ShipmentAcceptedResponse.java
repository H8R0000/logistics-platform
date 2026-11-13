package com.example.logistics.management.dto;

import com.example.logistics.management.entity.ShipmentStatus;
import java.util.UUID;

public record ShipmentAcceptedResponse(UUID id, ShipmentStatus status) {
}
