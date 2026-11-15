package com.example.logistics.courier.exception;

import java.util.UUID;

public class CourierNotFoundException extends RuntimeException {

    public CourierNotFoundException(UUID id) {
        super("Courier " + id + " not found");
    }
}
