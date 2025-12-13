package com.jobportal.backend.service;

import com.jobportal.backend.dto.security.request.VerifySignupRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface EmailVerificationService {

    void sendVerificationOtp(String email, String username, String passwordHash,
                             String roles, HttpServletRequest request);

    PendingRegistration verifyOtp(VerifySignupRequest verifyRequest, HttpServletRequest request);

    void resendVerificationOtp(String email, HttpServletRequest request);

    void cleanupExpiredRegistrations();

    class PendingRegistration {
        private final String email;
        private final String fullName;
        private final String passwordHash;
        private final String roles;

        public PendingRegistration(String email, String fullName,
                                   String passwordHash, String roles) {
            this.email = email;
            this.fullName = fullName;
            this.passwordHash = passwordHash;
            this.roles = roles;
        }

        public String getEmail() {
            return email;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPasswordHash() {
            return passwordHash;
        }

        public String getRoles() {
            return roles;
        }
    }
}