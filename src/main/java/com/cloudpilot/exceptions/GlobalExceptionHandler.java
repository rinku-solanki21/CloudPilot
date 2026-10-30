package com.cloudpilot.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler
 * 
 * Intercepts uncaught application exceptions and converts them into structured 
 * JSON error responses for the frontend dashboard.
 * 
 * Demonstrates Spring Boot REST exception handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxClusterCapacityException.class)
    public ResponseEntity<Map<String, Object>> handleMaxCapacity(MaxClusterCapacityException ex) {
        Map<String, Object> error = buildErrorBody(HttpStatus.BAD_REQUEST, "MAX_CAPACITY_REACHED", ex.getMessage());
        error.put("currentCapacity", ex.getCurrentCapacity());
        error.put("maxCapacity", ex.getMaxCapacity());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NoHealthyVMException.class)
    public ResponseEntity<Map<String, Object>> handleNoHealthyVM(NoHealthyVMException ex) {
        Map<String, Object> error = buildErrorBody(HttpStatus.SERVICE_UNAVAILABLE, "NO_HEALTHY_VM", ex.getMessage());
        error.put("totalVmsChecked", ex.getTotalVmsChecked());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> error = buildErrorBody(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> error = buildErrorBody(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private Map<String, Object> buildErrorBody(HttpStatus status, String errorCode, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        body.put("status", status.value());
        body.put("errorCode", errorCode);
        body.put("message", message);
        body.put("success", false);
        return body;
    }
}
