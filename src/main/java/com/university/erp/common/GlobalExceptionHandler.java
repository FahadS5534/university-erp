package com.university.erp.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus (HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(
        IllegalArgumentException ex) {
            return Map.of(
                "timestamp",LocalDateTime.now(),
                "error",ex.getMessage());
        }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object>handleConflict(IllegalStateException ex){
        return Map.of(
            "timestamp",LocalDateTime.now(),
                "error",ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", ex.getReason() == null ? status.getReasonPhrase() : ex.getReason()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleNotFound(
            RuntimeException ex) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "error", "Internal server error"
        );
    }
}
