package com.jobportal.backend.service;

import com.jobportal.backend.entity.User;

public interface EmailService {
    void sendPasswordResetEmail(User user, String token);
    void sendWelcomeEmail(User user);
}