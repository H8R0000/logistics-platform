package com.example.logistics.management.exception;

import java.util.UUID;

public class ShipmentNotFoundException extends RuntimeException {

    public ShipmentNotFoundException(UUID id) {
        super("Shipment " + id + " was not found");
    }
}
