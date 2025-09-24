package com.jobportal.backend.dto.security.request;

import lombok.Data;

@Data
public class TokenRefreshRequest {
    private String refreshToken;
}