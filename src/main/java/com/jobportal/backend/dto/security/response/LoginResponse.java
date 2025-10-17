package com.jobportal.backend.dto.security.response;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private Collection<? extends GrantedAuthority> roles;
    private Long expiresIn;

    public LoginResponse(String accessToken, String refreshToken, Long userId,
                         String username, String email, Collection<? extends GrantedAuthority> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}