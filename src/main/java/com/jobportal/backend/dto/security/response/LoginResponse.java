package com.jobportal.backend.dto.security.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime timestamp;

    public LoginResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.timestamp = LocalDateTime.now();
    }
}