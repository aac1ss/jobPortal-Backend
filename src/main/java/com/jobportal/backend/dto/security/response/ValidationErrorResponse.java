package com.jobportal.backend.dto.security.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {
    private boolean success = false;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    private Map<String, String> validationErrors;
    private String path;

    public ValidationErrorResponse(String message, Map<String, String> validationErrors, String path) {
        this.message = message;
        this.validationErrors = validationErrors;
        this.path = path;
    }
}