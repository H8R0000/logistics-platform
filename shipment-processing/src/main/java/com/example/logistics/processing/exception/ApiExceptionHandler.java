package com.example.logistics.processing.exception;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@NullMarked
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleNotFound(TaskNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ProblemDetail handleInvalidQuery(InvalidQueryException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(TaskRetryConflictException.class)
    public ProblemDetail handleRetryConflict(TaskRetryConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
