package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.security.request.ForgotPasswordRequest;
import com.jobportal.backend.dto.security.request.ResetPasswordRequest;
import com.jobportal.backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password")
@Tag(name = "2. Password Management", description = "APIs for password reset functionality")
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Request password reset", description = "Send password reset instructions to user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reset instructions sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email address"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/forgot")
    public GenericResponse<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                             HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        passwordResetService.requestPasswordReset(request.getEmail(), ipAddress, userAgent);

        return GenericResponse.success("Password reset instructions have been sent to your email");
    }

    @Operation(summary = "Reset password", description = "Reset user password using valid reset token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid token or password"),
            @ApiResponse(responseCode = "404", description = "Reset token not found")
    })
    @PostMapping("/reset")
    public GenericResponse<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                            HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), ipAddress, userAgent);

        return GenericResponse.success("Password has been reset successfully");
    }
}