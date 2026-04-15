package com.jobportal.backend.service;

import com.jobportal.backend.dto.security.request.ChangePasswordRequest;
import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.request.VerifySignupRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;
import com.jobportal.backend.entity.User;

public interface AuthService {
    LoginResponse authenticateCandidate(LoginRequest loginRequest);
    LoginResponse authenticateRecruiter(LoginRequest loginRequest);
    LoginResponse authenticateAdmin(LoginRequest loginRequest);
    void registerUser(SignupRequest signUpRequest);
    User verifyAndCompleteSignup(VerifySignupRequest verifyRequest);
    void resendVerificationOtp(String email);
    void logout(String refreshToken);
    void changePassword(ChangePasswordRequest request, String authenticatedEmail);
}