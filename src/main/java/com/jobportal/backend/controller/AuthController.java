package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.request.TokenRefreshRequest;
import com.jobportal.backend.dto.security.request.VerifySignupRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;
import com.jobportal.backend.dto.security.response.SignupResponse;
import com.jobportal.backend.dto.security.response.TokenRefreshResponse;
import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.entity.User;
import com.jobportal.backend.exception.TokenNotFoundException;
import com.jobportal.backend.security.JwtUtils;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.AuthService;
import com.jobportal.backend.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "Candidate login", description = "Login for candidate users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or user is not a candidate")
    })
    @PostMapping("/candidate/login")
    public ResponseEntity<GenericResponse<LoginResponse>> candidateLogin(
            @Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticateCandidate(loginRequest);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Recruiter login", description = "Login for recruiter users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or user is not a recruiter")
    })
    @PostMapping("/recruiter/login")
    public ResponseEntity<GenericResponse<LoginResponse>> recruiterLogin(
            @Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticateRecruiter(loginRequest);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Admin login", description = "Login for admin users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or user is not an admin")
    })
    @PostMapping("/admin/login")
    public ResponseEntity<GenericResponse<LoginResponse>> adminLogin(
            @Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticateAdmin(loginRequest);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "User registration", description = "Register a new user (sends verification OTP)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/signup")
    public ResponseEntity<GenericResponse<SignupResponse>> registerUser(
            @Valid @RequestBody SignupRequest signUpRequest) {

        authService.registerUser(signUpRequest);

        SignupResponse response = SignupResponse.builder()
                .email(signUpRequest.getEmail())
                .username(signUpRequest.getUsername())
                .message("Verification code sent to your email. Please check your inbox.")
                .build();

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Verify signup", description = "Verify email OTP to complete registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or email"),
            @ApiResponse(responseCode = "401", description = "Invalid verification code"),
            @ApiResponse(responseCode = "404", description = "Verification not found")
    })
    @PostMapping("/verify-signup")
    public ResponseEntity<GenericResponse<?>> verifySignup(
            @Valid @RequestBody VerifySignupRequest verifyRequest) {

        User user = authService.verifyAndCompleteSignup(verifyRequest);

        return ResponseEntity.ok(GenericResponse.success(
                String.format("Registration completed successfully! Welcome %s", user.getUsername())
        ));
    }

    @Operation(summary = "Resend verification OTP",
            description = "Resend verification OTP (2-minute cooldown)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP resent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email or cooldown active"),
            @ApiResponse(responseCode = "404", description = "No pending registration found")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<GenericResponse<?>> resendVerification(
            @RequestParam String email) {

        authService.resendVerificationOtp(email);

        return ResponseEntity.ok(GenericResponse.success(
                "Verification code has been resent to your email."
        ));
    }



    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "403", description = "Invalid refresh token")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<GenericResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {

        String requestRefreshToken = request.getRefreshToken();

        TokenRefreshResponse response = refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateTokenFromEmail(user.getEmail());
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new TokenNotFoundException("Invalid refresh token"));

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Logout", description = "Logout user by invalidating refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token")
    })
    @PostMapping("/signout")
    public ResponseEntity<GenericResponse<?>> logoutUser(
            @RequestParam("refreshToken") String refreshToken) {

        authService.logout(refreshToken);
        return ResponseEntity.ok(GenericResponse.success("Logout successful"));
    }
}