package com.example.logistics.processing.exception;

public class TaskRetryConflictException extends RuntimeException {

    public TaskRetryConflictException(String message) {
        super(message);
    }
}
