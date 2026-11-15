package com.example.logistics.processing.exception;

public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
