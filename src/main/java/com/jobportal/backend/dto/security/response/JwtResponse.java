package com.jobportal.backend.dto.security.response;

import lombok.Data;

import java.util.Collection;

@Data
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private Collection<String> roles;

    public JwtResponse(String accessToken, String refreshToken, Long id, String username, String email, Collection<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}