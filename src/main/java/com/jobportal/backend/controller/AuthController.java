package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.request.TokenRefreshRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;
import com.jobportal.backend.dto.security.response.TokenRefreshResponse;
import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.exception.TokenNotFoundException;
import com.jobportal.backend.security.JwtUtils;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.AuthService;
import com.jobportal.backend.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "APIs for user authentication and authorization")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtUtils jwtUtils) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtils = jwtUtils;
    }

    @Operation(summary = "User login", description = "Authenticate user with email and password to get JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = GenericResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/signin")
    public ResponseEntity<GenericResponse<LoginResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "User registration", description = "Register a new user account with specified roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "400", description = "Username or email already exists")
    })
    @PostMapping("/signup")
    public ResponseEntity<GenericResponse<?>> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        authService.registerUser(signUpRequest);
        return ResponseEntity.ok(GenericResponse.success("User registered successfully!"));
    }

    @Operation(summary = "Refresh access token", description = "Get new access token using valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "404", description = "Refresh token not found"),
            @ApiResponse(responseCode = "403", description = "Refresh token expired")
    })
    @PostMapping("/refreshtoken")
    public ResponseEntity<GenericResponse<TokenRefreshResponse>> refreshtoken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        TokenRefreshResponse response = refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateTokenFromEmail(user.getEmail());
                    return new TokenRefreshResponse(token, requestRefreshToken);
                })
                .orElseThrow(() -> new TokenNotFoundException(
                        "Refresh token is not in database!"));

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "User logout", description = "Logout user and invalidate refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/signout")
    public ResponseEntity<GenericResponse<?>> logoutUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userPrincipal.getId();
        refreshTokenService.deleteByUserId(userId);
        return ResponseEntity.ok(GenericResponse.success("Log out successful!"));
    }
}