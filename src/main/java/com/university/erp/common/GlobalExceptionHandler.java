package com.university.erp.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(
            RuntimeException ex) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "error", ex.getMessage()
        );
    }
}
