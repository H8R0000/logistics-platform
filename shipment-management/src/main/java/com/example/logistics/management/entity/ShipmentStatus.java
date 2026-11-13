package com.example.logistics.management.entity;

public enum ShipmentStatus {
    CREATED,
    PROCESSING,
    WAITING_FOR_COURIER,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    COURIER_NOT_FOUND
}
