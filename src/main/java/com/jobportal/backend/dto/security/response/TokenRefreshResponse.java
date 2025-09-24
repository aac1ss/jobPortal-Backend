package com.jobportal.backend.dto.security.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime timestamp;

    public TokenRefreshResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.timestamp = LocalDateTime.now();
    }
}