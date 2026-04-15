package com.jobportal.backend.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException {
    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}