package com.jobportal.backend.service;

public interface PasswordResetService {
    void requestPasswordReset(String email, String ipAddress, String userAgent);
    void resetPassword(String token, String newPassword, String ipAddress, String userAgent);
}