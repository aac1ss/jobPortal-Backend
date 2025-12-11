package com.jobportal.backend.service;

import com.jobportal.backend.entity.User;

public interface EmailService {
    void sendPasswordResetEmail(User user, String token);
    void sendWelcomeEmail(User user);
    void sendVerificationEmail(String email, String name, String otp);
}