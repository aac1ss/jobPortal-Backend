package com.jobportal.backend.exception;

import com.jobportal.backend.dto.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        GenericResponse<Map<String, String>> response = GenericResponse.error("Please fix the validation errors");
        response.setData(errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<GenericResponse<String>> handleBaseException(BaseException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        GenericResponse<String> response = GenericResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<GenericResponse<String>> handleBadCredentialsException() {
        log.warn("Bad credentials attempt");
        GenericResponse<String> response = GenericResponse.error("Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<GenericResponse<String>> handleLockedException() {
        log.warn("Locked account attempt");
        GenericResponse<String> response = GenericResponse.error("Account is locked");
        return ResponseEntity.status(HttpStatus.LOCKED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse<String>> handleAccessDeniedException() {
        log.warn("Access denied");
        GenericResponse<String> response = GenericResponse.error("Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse<String>> handleHttpMessageNotReadable() {
        log.warn("Invalid JSON request");
        GenericResponse<String> response = GenericResponse.error("Invalid request format");
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<String>> handleGlobalException(Exception ex) {
        log.error("Unexpected error", ex);
        GenericResponse<String> response = GenericResponse.error("An unexpected error occurred");
        return ResponseEntity.internalServerError().body(response);
    }
}