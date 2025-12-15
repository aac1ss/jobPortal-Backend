package com.jobportal.backend.exception;

import org.springframework.http.HttpStatus;

public class ProfileIncompleteException extends BaseException {
    public ProfileIncompleteException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}