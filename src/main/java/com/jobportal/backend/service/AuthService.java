package com.jobportal.backend.service;

import com.jobportal.backend.dto.auth.request.LoginRequest;
import com.jobportal.backend.dto.auth.request.RefreshTokenRequest;
import com.jobportal.backend.dto.auth.request.RegisterRequest;
import com.jobportal.backend.dto.auth.response.AuthResponse;
import com.jobportal.backend.dto.auth.response.TokenRefreshResponse;
import com.jobportal.backend.entity.User;

public interface AuthService {
    AuthResponse register(RegisterRequest request, String role);
    AuthResponse login(LoginRequest request);
    TokenRefreshResponse refreshToken(RefreshTokenRequest request);
    void logout(String token);
    void verifyEmail(String token);
    void invalidateUserTokens(User user);
}