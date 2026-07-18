package com.testgenai.controller;

import com.testgenai.dto.GenerateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenerateResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Invalid request"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return ResponseEntity.badRequest().body(GenerateResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenerateResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(500).body(GenerateResponse.fail("Unexpected error: " + ex.getMessage()));
    }
}
