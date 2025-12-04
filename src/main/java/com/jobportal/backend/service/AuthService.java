package com.jobportal.backend.service;

import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;

public interface AuthService {
    LoginResponse authenticateCandidate(LoginRequest loginRequest);
    LoginResponse authenticateRecruiter(LoginRequest loginRequest);
    LoginResponse authenticateAdmin(LoginRequest loginRequest);
    void registerUser(SignupRequest signUpRequest);
}