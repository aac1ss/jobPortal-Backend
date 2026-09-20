package com.jobportal.backend.exception;

import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends BaseException {
    public ProfileNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}