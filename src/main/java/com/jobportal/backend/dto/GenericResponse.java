package com.jobportal.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GenericResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public GenericResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> GenericResponse<T> success(T data) {
        return new GenericResponse<>(true, "Operation successful", data);
    }

    public static <T> GenericResponse<T> success(String message, T data) {
        return new GenericResponse<>(true, message, data);
    }

    public static <T> GenericResponse<T> error(String message) {
        return new GenericResponse<>(false, message, null);
    }
}