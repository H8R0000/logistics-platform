package com.example.logistics.processing.kafka;

public class InvalidProcessingCommandException extends RuntimeException {

    public InvalidProcessingCommandException(String message) {
        super(message);
    }

    public InvalidProcessingCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
