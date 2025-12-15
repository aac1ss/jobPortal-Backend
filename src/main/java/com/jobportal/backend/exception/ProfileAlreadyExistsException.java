package com.jobportal.backend.exception;

import org.springframework.http.HttpStatus;

public class ProfileAlreadyExistsException extends BaseException {
    public ProfileAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}