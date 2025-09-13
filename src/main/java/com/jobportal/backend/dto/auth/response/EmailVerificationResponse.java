package com.jobportal.backend.dto.auth.response;

public class EmailVerificationResponse {
    private String message;

    public EmailVerificationResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}