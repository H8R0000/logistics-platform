package com.example.logistics.management.kafka;

public class InvalidExternalShipmentRequestException extends RuntimeException {

    public InvalidExternalShipmentRequestException(String message) {
        super(message);
    }

    public InvalidExternalShipmentRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
