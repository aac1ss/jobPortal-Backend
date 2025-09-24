package com.jobportal.backend.controller;

import com.jobportal.backend.dto.ApiResponse;
import com.jobportal.backend.dto.security.request.ForgotPasswordRequest;
import com.jobportal.backend.dto.security.request.ResetPasswordRequest;
import com.jobportal.backend.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/password")
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot")
    public ApiResponse<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                         HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        passwordResetService.requestPasswordReset(request.getEmail(), ipAddress, userAgent);

        return ApiResponse.success("Password reset instructions have been sent to your email");
    }

    @PostMapping("/reset")
    public ApiResponse<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                        HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), ipAddress, userAgent);

        return ApiResponse.success("Password has been reset successfully");
    }
}