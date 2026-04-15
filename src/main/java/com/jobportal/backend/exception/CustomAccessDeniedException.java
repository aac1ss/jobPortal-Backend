package com.jobportal.backend.exception;

import org.springframework.http.HttpStatus;

public class CustomAccessDeniedException extends BaseException {
    public CustomAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}