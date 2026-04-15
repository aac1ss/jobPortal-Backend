package com.jobportal.backend.exception;

import org.springframework.http.HttpStatus;

public class FileUploadException extends BaseException {
    public FileUploadException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}